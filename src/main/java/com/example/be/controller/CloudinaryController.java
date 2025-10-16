package com.example.be.controller;

import com.example.be.entity.InternDocument;
import com.example.be.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin("*")
public class CloudinaryController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload_cloud")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            @RequestParam("internProfileId") Long internProfileId,
                                            @RequestParam("hrId") Long hrId) {
        try {
            InternDocument doc = documentService.uploadDocument(file, internProfileId, hrId);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-url/{userId}")
    public ResponseEntity<?> getLatestFileUrl(@PathVariable Long userId) {

        return ResponseEntity.ok(documentService.getLatestFileUrlByUserId(userId));
    }

    @PutMapping("/{documentId}/accept")
    public ResponseEntity<?> acceptDocument(@PathVariable Long documentId,
                                            @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        System.out.println(">>> userId nhận được = " + userId);
        return ResponseEntity.ok(documentService.acceptDocument(documentId, userId));
    }


    @GetMapping("/contracts")
    public ResponseEntity<?> getAllContracts() {
        return ResponseEntity.ok(documentService.getAllContracts());
    }
}
