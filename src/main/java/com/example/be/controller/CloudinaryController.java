package com.example.be.controller;

import com.example.be.entity.InternDocument;
import com.example.be.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/get-url/{internId}")
    public ResponseEntity<?> getLatestFileUrl(@PathVariable Long internId) {
        return ResponseEntity.ok(documentService.getLatestFileUrlByInternId(internId));
    }

    @PutMapping("/{documentId}/accept")
    public ResponseEntity<?> acceptDocument(@PathVariable Long documentId,
                                            @RequestParam Long internId) {
        return ResponseEntity.ok(documentService.acceptDocument(documentId, internId));
    }

    @GetMapping("/contracts")
    public ResponseEntity<?> getAllContracts() {
        return ResponseEntity.ok(documentService.getAllContracts());
    }
}
