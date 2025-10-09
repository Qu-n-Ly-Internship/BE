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
import java.util.List;

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
                @RequestParam("hrId") Long hrId  // ✅ Thêm HR ID
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
            doc.setHr(hr); // ✅ Gắn HR upload
            doc.setDocumentName(file.getOriginalFilename());
            doc.setDocumentType(type);
            doc.setFileDetail(fileUrl);
            doc.setUploadedAt(LocalDateTime.now());
            doc.setStatus("PENDING");

            // 6️⃣ Lưu DB
            documentRepository.save(doc);

            return doc;
        }

    // ============================
    // LẤY URL FILE THEO INTERN_ID
    // ============================
    @GetMapping("/get-url/{internId}")
    public ResponseEntity<?> getFileUrlsByInternId(@PathVariable Long internId) {
        // Gọi repository để lấy danh sách URL
        List<String> fileUrls = documentRepository.findFileUrlsByInternId(internId);

        if (fileUrls == null || fileUrls.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy file cho intern_id: " + internId);
        }

        return ResponseEntity.ok(fileUrls);
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

}
