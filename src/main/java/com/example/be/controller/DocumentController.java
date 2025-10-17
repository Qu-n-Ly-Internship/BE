package com.example.be.controller;

import com.example.be.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // 1. Lấy tất cả tài liệu với filter
    @GetMapping("")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "q", defaultValue = "") String query
    ) {
        try {
            Map<String, Object> result = documentService.getAllDocuments(status, documentType, query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách tài liệu: " + e.getMessage()
            ));
        }
    }

    // 10. Lấy tài liệu của chính người dùng dựa vào uploaderEmail (không cần schema change)
    @GetMapping("/my")
    public ResponseEntity<?> getMyDocuments(@RequestParam("email") String email) {
        try {
            Map<String, Object> result = documentService.getMyDocuments(email);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu của bạn: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy tài liệu chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDocuments() {
        try {
            Map<String, Object> result = documentService.getPendingDocuments();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu chờ duyệt: " + e.getMessage()
            ));
        }
    }

    // 3. Lấy chi tiết một tài liệu
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {
        try {
            Map<String, Object> result = documentService.getDocumentById(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết tài liệu: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy tài liệu theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getDocumentsByIntern(@PathVariable Long internId) {
        try {
            Map<String, Object> result = documentService.getDocumentsByIntern(internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu của thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 5. Thống kê tài liệu theo trạng thái
    @GetMapping("/stats")
    public ResponseEntity<?> getDocumentStats() {
        try {
            Map<String, Object> result = documentService.getDocumentStats();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    // 6. Upload tài liệu mới (HỢP ĐỒNG, GIẤY TỜ - KHÔNG BAO GỒM CV)
    // CV sẽ được upload qua /api/cv/upload
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam(value = "internId", required = false) Long internId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploaderEmail", required = false) String uploaderEmail) {
        try {
            Map<String, Object> result = documentService.uploadDocument(internId, documentType, file, uploaderEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi upload tài liệu: " + e.getMessage()
            ));
        }
    }

    // 7. Duyệt tài liệu
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveDocument(@PathVariable Long id) {
        try {
            Map<String, Object> result = documentService.approveDocument(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi duyệt tài liệu: " + e.getMessage()
            ));
        }
    }

    // 8. Từ chối tài liệu
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng cung cấp lý do từ chối"
                ));
            }

            Map<String, Object> result = documentService.rejectDocument(id, reason.trim());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi từ chối tài liệu: " + e.getMessage()
            ));
        }
    }

    // 9. API tổng hợp cho HR - duyệt/từ chối trong 1 endpoint
    @PutMapping("/{id}/review")
    public ResponseEntity<?> reviewDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            Map<String, Object> result = documentService.reviewDocument(id, request.get("action"), request.get("note"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xử lý tài liệu thất bại: " + e.getMessage()
            ));
        }
    }

    // 10. Upload tài liệu cho intern cụ thể (HR upload hợp đồng)
    @PostMapping("/upload-for-intern")
    public ResponseEntity<?> uploadForIntern(
            @RequestParam("internId") Long internId,
            @RequestParam("type") String documentType,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Map<String, Object> result = documentService.uploadForIntern(internId, documentType, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tải lên thất bại: " + e.getMessage()
            ));
        }
    }

    // 11. Xác nhận hợp đồng (Intern xác nhận đã đọc và đồng ý)
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmDocument(@PathVariable Long id) {
        try {
            Map<String, Object> result = documentService.confirmDocument(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận thất bại: " + e.getMessage()
            ));
        }
    }

    // 12. Xóa tài liệu
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            Map<String, Object> result = documentService.deleteDocument(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa tài liệu thất bại: " + e.getMessage()
            ));
        }
    }
}