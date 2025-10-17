package com.example.be.service;

import com.example.be.entity.Hr;
import com.example.be.entity.InternDocument;
import com.example.be.entity.InternProfile;
import com.example.be.repository.DocumentRepository;
import com.example.be.repository.HrRepository;
import com.example.be.repository.InternProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private CloudinaryRestService cloudinaryRestService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private HrRepository hrRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ====================================================
    // 1️⃣ UPLOAD FILE LÊN CLOUDINARY + LƯU DB
    // ====================================================
    public InternDocument uploadDocument(MultipartFile file, Long internProfileId, Long hrId) throws IOException {
        // Upload file lên Cloudinary
        String response = cloudinaryRestService.uploadFile(file);

        // Parse JSON để lấy thông tin trả về
        JsonNode json = objectMapper.readTree(response);
        String fileUrl = json.get("secure_url").asText();
        String type = json.get("resource_type").asText();

        InternProfile internProfile = internProfileRepository.findById(internProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy internProfile có ID: " + internProfileId));

        Hr hr = hrRepository.findByUser_Id(hrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy HR tương ứng với user_id: " + hrId));

        InternDocument doc = new InternDocument();
        doc.setInternProfile(internProfile);
        doc.setHr(hr);
        doc.setDocumentName(file.getOriginalFilename());
        doc.setDocumentType(type);
        doc.setFileDetail(fileUrl);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus("PENDING");
        doc.setReviewedAt(LocalDateTime.now());

        // Lưu vào DB
        return documentRepository.save(doc);
    }


    // ====================================================
    // 2️⃣ LẤY URL FILE MỚI NHẤT THEO INTERN ID
    // ====================================================
public Map<String, Object> getLatestFileUrlByUserId(Long userId) {
        // 🔹 Lấy internId thực tế từ userId
        InternProfile internProfile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy InternProfile cho user ID: " + userId));

        Long internId = internProfile.getId();

        // 🔹 Tìm document mới nhất theo internId
        InternDocument document = documentRepository.findTopByInternProfile_IdOrderByUploadedAtDesc(internId);

        if (document == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy file cho intern_id: " + internId);
        }

        // 🔹 Lấy thông tin HR (nếu có)
        Hr hr = document.getHr();
        Long hrId = (hr != null) ? hr.getId() : null;
        String hrName = (hr != null) ? hr.getFullname() : null;

        // 🔹 Chuẩn bị response
        Map<String, Object> response = new HashMap<>();
        response.put("document_id", document.getId());
        response.put("hr_id", hrId);
        response.put("name_hr", hrName);
        response.put("file_url", document.getFileDetail());
        response.put("uploaded_at", document.getUploadedAt());
        response.put("status", document.getStatus());

        return response;
    }


    // ====================================================
    // 3️⃣ XÁC NHẬN HỢP ĐỒNG
    // ====================================================
    public String acceptDocument(Long documentId, Long userId) {
        InternDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy document với ID: " + documentId));

        // 🔹 Tìm InternProfile từ user_id
        InternProfile internProfile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy InternProfile tương ứng với user_id: " + userId));

        Long internId = internProfile.getId(); // 🌟 đây chính là cầu nối từ user_id → intern_id

        // 🔹 Kiểm tra quyền sở hữu
        if (!doc.getInternProfile().getId().equals(internId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Document này không thuộc về intern ID: " + internId);
        }

        // 🔹 Kiểm tra trạng thái
        if (!"PENDING".equals(doc.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hợp đồng đã được xử lý rồi!");
        }

        // 🔹 Cập nhật trạng thái
        doc.setStatus("ACCEPTED");
        doc.setReviewedAt(LocalDateTime.now());
        documentRepository.save(doc);
return "Hợp đồng đã được xác nhận thành công.";
    }




    // ====================================================
    // 4️⃣ LẤY DANH SÁCH TẤT CẢ HỢP ĐỒNG (HR DASHBOARD)
    // ====================================================
    public List<Map<String, Object>> getAllContracts() {
        List<InternProfile> interns = internProfileRepository.findAll();
        List<InternDocument> latestDocs = documentRepository.findLatestDocumentPerIntern();

        Map<Long, InternDocument> docMap = latestDocs.stream()
                .collect(Collectors.toMap(
                        d -> d.getInternProfile().getId(),
                        d -> d
                ));

        List<Map<String, Object>> result = new ArrayList<>();

        for (InternProfile intern : interns) {
            Map<String, Object> data = new HashMap<>();
            data.put("intern_id", intern.getId());
            data.put("intern_name", intern.getFullName());
            data.put("university", intern.getUniversity() != null
                    ? intern.getUniversity().getName()
                    : "-");

            InternDocument doc = docMap.get(intern.getId());
            if (doc != null) {
                data.put("document_id", doc.getId());
                data.put("file_name", doc.getDocumentName());
                data.put("file_url", doc.getFileDetail());
                data.put("status", doc.getStatus());
                data.put("uploaded_at", doc.getUploadedAt());
                data.put("hr_name", doc.getHr() != null ? doc.getHr().getFullname() : "-");
            } else {
                data.put("document_id", null);
                data.put("file_name", "-");
                data.put("file_url", "-");
                data.put("status", "Chưa có hợp đồng");
                data.put("uploaded_at", null);
                data.put("hr_name", "-");
            }

            result.add(data);
        }

        return result;
    }
}
