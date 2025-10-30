package com.example.be.controller;

import com.example.be.dto.SupportRequestDTO;
import com.example.be.entity.SupportRequest;
import com.example.be.service.SupportRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportRequestController {
    private final SupportRequestService supportRequestService;

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody SupportRequestDTO dto) {
        try {
            SupportRequest response = supportRequestService.createRequest(dto);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> createRequestWithFile(
            @RequestParam("userId") Integer userId, // ⭐ ĐỔI parameter
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            @RequestParam(value = "priority", defaultValue = "NORMAL") String priority,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            System.out.println("=== CONTROLLER RECEIVED ===");
            System.out.println("userId: " + userId);
            System.out.println("subject: " + subject);
            System.out.println("message length: " + (message != null ? message.length() : 0));
            System.out.println("priority: " + priority);
            System.out.println("file: " + (file != null ? file.getOriginalFilename() : "null"));

            // Validate
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "userId là bắt buộc"));
            }
            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "subject là bắt buộc"));
            }
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "message là bắt buộc"));
            }

            SupportRequest response = supportRequestService.createRequestWithFile(
                    userId, subject, message, priority, file);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy) {
        try {
            Map<String, Object> response = supportRequestService.getAllRequests(page, size, sortBy);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Giữ endpoint cũ cho backward compatibility
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getRequestsByIntern(@PathVariable Integer internId) {
        try {
            List<SupportRequest> requests = supportRequestService.getRequestsByIntern(internId);
            return ResponseEntity.ok(Map.of("success", true, "data", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ⭐ THÊM endpoint mới: Query theo user_id
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getRequestsByUser(@PathVariable Integer userId) {
        try {
            List<SupportRequest> requests = supportRequestService.getRequestsByUser(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/hr/{hrId}")
    public ResponseEntity<?> getRequestsByHr(@PathVariable Long hrId) {
        try {
            List<SupportRequest> requests = supportRequestService.getRequestsByHr(hrId);
            return ResponseEntity.ok(Map.of("success", true, "data", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        try {
            SupportRequest response = supportRequestService.getRequestById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{requestId}/process")
    public ResponseEntity<?> processRequest(
            @PathVariable Long requestId,
            @RequestParam Long hrId,
            @RequestBody Map<String, String> body) {
        try {
            String response = body.get("response");
            String status = body.get("status");
            SupportRequest result = supportRequestService.processRequest(requestId, hrId, response, status);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getRequestsByStatus(@PathVariable String status) {
        try {
            List<SupportRequest> requests = supportRequestService.getRequestsByStatus(status);
            return ResponseEntity.ok(Map.of("success", true, "data", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}