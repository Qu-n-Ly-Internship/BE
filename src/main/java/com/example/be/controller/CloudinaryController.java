package com.example.be.controller;

import com.example.be.entity.InternDocument;
import com.example.be.entity.InternProfile;
import  com.example.be.entity.Hr;
import com.example.be.repository.DocumentRepository;
import com.example.be.repository.HrRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.service.CloudinaryRestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin("*")
public class CloudinaryController {

    @Autowired
    private CloudinaryRestService cloudinaryRestService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private HrRepository hrRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================
    // UPLOAD FILE LÊN CLOUDINARY
    // ============================
    @PostMapping("/upload_cloud")
    public InternDocument uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("internProfileId") Long internProfileId,
            @RequestParam("hrId") Long hrId
    ) throws IOException {

        // 1️⃣ Upload file lên Cloudinary
        String response = cloudinaryRestService.uploadFile(file);

        // 2️⃣ Parse JSON để lấy URL và loại file
        JsonNode json = objectMapper.readTree(response);
        String fileUrl = json.get("secure_url").asText();
        String type = json.get("resource_type").asText();

        // 3️⃣ Tìm InternProfile
        InternProfile internProfile = internProfileRepository.findById(internProfileId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy internProfile có ID: " + internProfileId));

        // 4️⃣ Tìm HR theo hrId
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR có ID: " + hrId));

        // 5️⃣ Tạo mới InternDocument
        InternDocument doc = new InternDocument();
        doc.setInternProfile(internProfile);
        doc.setHr(hr);
        doc.setDocumentName(file.getOriginalFilename());
        doc.setDocumentType(type);
        doc.setFileDetail(fileUrl);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus("PENDING");

        // ✅ Hiển thị luôn thời gian reviewedAt (lúc upload)
        doc.setReviewedAt(LocalDateTime.now());

        // 6️⃣ Lưu DB
        documentRepository.save(doc);

        return doc;
    }


    // ============================
    // LẤY URL FILE THEO INTERN_ID
    // ============================
    @GetMapping("/get-url/{internId}")
    public ResponseEntity<?> getLatestFileUrlByInternId(@PathVariable Long internId) {
        // Lấy document mới nhất của intern
        InternDocument document = documentRepository.findTopByInternProfile_IdOrderByUploadedAtDesc(internId);

        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy file cho intern_id: " + internId);
        }

        // Lấy thông tin HR
        Hr hr = document.getHr();
        Long hrId = (hr != null) ? hr.getId() : null;
        String hrName = (hr != null) ? hr.getFullname() : null;

        // Tạo response JSON
        Map<String, Object> response = new HashMap<>();
        response.put("document_id", document.getId()); // ✅ THÊM DÒNG NÀY
        response.put("hr_id", hrId);
        response.put("name_hr", hrName);
        response.put("file_url", document.getFileDetail());
        response.put("uploaded_at", document.getUploadedAt());
        response.put("status", document.getStatus());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{documentId}/accept")
    public ResponseEntity<?> acceptDocument(
            @PathVariable Long documentId,
            @RequestParam("internId") Long internId
    ) {
        InternDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy document với ID: " + documentId));

        // Kiểm tra document có thuộc về intern này không
        if (!doc.getInternProfile().getId().equals(internId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Document này không thuộc về intern ID: " + internId);
        }

        // Kiểm tra trạng thái
        if (!"PENDING".equals(doc.getStatus())) {
            return ResponseEntity.badRequest().body("Hợp đồng đã được xử lý rồi!");
        }

        // Cập nhật trạng thái
        doc.setStatus("ACCEPTED");
        doc.setReviewedAt(LocalDateTime.now());
        documentRepository.save(doc);

        return ResponseEntity.ok("Hợp đồng đã được xác nhận thành công.");
    }


    @GetMapping("/contracts")
    public ResponseEntity<?> getAllContracts() {

        // 1️⃣ Lấy tất cả interns
        List<InternProfile> interns = internProfileRepository.findAll();

        // 2️⃣ Lấy document mới nhất mỗi intern
        List<InternDocument> latestDocs = documentRepository.findLatestDocumentPerIntern();

        // 3️⃣ Map internId → InternDocument
        Map<Long, InternDocument> docMap = latestDocs.stream()
                .collect(Collectors.toMap(
                        d -> d.getInternProfile().getId(),
                        d -> d
                ));

        // 4️⃣ Kết hợp dữ liệu
        List<Map<String, Object>> result = new ArrayList<>();

        for (InternProfile intern : interns) {
            Map<String, Object> data = new HashMap<>();
            data.put("intern_id", intern.getId());
            data.put("intern_name", intern.getFullName());
            data.put("university", intern.getUniversity() != null ? intern.getUniversity().getName() : "-");

            InternDocument doc = docMap.get(intern.getId());
            if (doc != null) {
                data.put("document_id", doc.getId());
                data.put("file_name", doc.getDocumentName());
                data.put("file_url", doc.getFileDetail());
                data.put("status", doc.getStatus());
                data.put("uploaded_at", doc.getUploadedAt());
                data.put("hr_name", doc.getHr() != null ? doc.getHr().getFullname() : "-");
            } else {
                // ❌ Nếu intern chưa có hợp đồng
                data.put("document_id", null);
                data.put("file_name", "-");
                data.put("file_url", "-");
                data.put("status", "Chưa có hợp đồng");
                data.put("uploaded_at", null);
                data.put("hr_name", "-");
            }

            result.add(data);
        }

        return ResponseEntity.ok(result);
    }


}
