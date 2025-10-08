package com.example.be.controller;

import com.example.be.entity.InternDocument;
import com.example.be.entity.InternProfile;
import com.example.be.repository.InternDocumentRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.service.CloudinaryRestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin("*")
public class CloudinaryController {

    @Autowired
    private CloudinaryRestService cloudinaryRestService;

    @Autowired
    private InternDocumentRepository documentRepository;

    @Autowired
    private InternProfileRepository internProfileRepository; // 🔹 thêm repo này để lấy internProfile

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/upload_cloud")
    public InternDocument uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("internProfileId") Long internProfileId // 🔹 nhận từ Postman
    ) throws IOException {

        // 🔹 Upload file lên Cloudinary
        String response = cloudinaryRestService.uploadFile(file);

        // 🔹 Parse JSON trả về để lấy URL và type
        JsonNode json = objectMapper.readTree(response);
        String fileUrl = json.get("secure_url").asText();
        String type = json.get("resource_type").asText();

        // 🔹 Tìm internProfile theo ID (nếu không có thì báo lỗi)
        InternProfile internProfile = internProfileRepository.findById(internProfileId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy internProfile có ID: " + internProfileId));

        // 🔹 Tạo document và gán thông tin
        InternDocument doc = new InternDocument();
        doc.setInternProfile(internProfile);
        doc.setDocumentName(file.getOriginalFilename());
        doc.setDocumentType(type);
        doc.setFileDetail(fileUrl);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus("PENDING");

        // 🔹 Lưu vào DB
        documentRepository.save(doc);

        return doc;
    }
}
