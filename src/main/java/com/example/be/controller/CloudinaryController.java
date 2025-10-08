package com.example.be.controller;

import com.example.be.entity.InternDocument;
import com.example.be.entity.InternProfile;
import com.example.be.repository.DocumentRepository;
import com.example.be.repository.InternDocumentRepository;
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
import java.util.Optional;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================
    // UPLOAD FILE LÊN CLOUDINARY
    // ============================
    @PostMapping("/upload_cloud")
    public InternDocument uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("internProfileId") Long internProfileId
    ) throws IOException {

        // Upload file lên Cloudinary
        String response = cloudinaryRestService.uploadFile(file);

        // Parse JSON để lấy URL và loại file
        JsonNode json = objectMapper.readTree(response);
        String fileUrl = json.get("secure_url").asText();
        String type = json.get("resource_type").asText();

        // Tìm InternProfile
        InternProfile internProfile = internProfileRepository.findById(internProfileId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy internProfile có ID: " + internProfileId));

        // Tạo mới InternDocument
        InternDocument doc = new InternDocument();
        doc.setInternProfile(internProfile);
        doc.setDocumentName(file.getOriginalFilename());
        doc.setDocumentType(type);
        doc.setFileDetail(fileUrl);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus("PENDING");

        // Lưu DB
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

}
