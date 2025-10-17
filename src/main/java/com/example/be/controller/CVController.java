package com.example.be.controller;

import com.example.be.service.CVService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CVController {

    private final CVService cvService;

    // 1. Lấy tất cả CV với filter
    @GetMapping("")
    public ResponseEntity<?> getAllCVs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", defaultValue = "") String query
    ) {
        try {
            Map<String, Object> result = cvService.getAllCVs(status, query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách CV: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy CV của chính người dùng dựa vào email
    @GetMapping("/my")
    public ResponseEntity<?> getMyCVs(@RequestParam("email") String email) {
        try {
            Map<String, Object> result = cvService.getMyCVs(email);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV của bạn: " + e.getMessage()
            ));
        }
    }

    // 3. Lấy CV chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCVs() {
        try {
            Map<String, Object> result = cvService.getPendingCVs();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV chờ duyệt: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy chi tiết một CV
    @GetMapping("/{id}")
    public ResponseEntity<?> getCVById(@PathVariable Long id) {
        try {
            Map<String, Object> result = cvService.getCVById(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết CV: " + e.getMessage()
            ));
        }
    }

    // 5. Lấy CV theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getCVsByIntern(@PathVariable Long internId) {
        try {
            Map<String, Object> result = cvService.getCVsByIntern(internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV của thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 6. Thống kê CV theo trạng thái
    @GetMapping("/stats")
    public ResponseEntity<?> getCVStats() {
        try {
            Map<String, Object> result = cvService.getCVStats();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    // 7. Upload CV mới
    @PostMapping("/upload")
    public ResponseEntity<?> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "internId", required = false) Long internId,
            @RequestParam(value = "uploaderEmail", required = false) String uploaderEmail
    ) {
        try {
            Map<String, Object> result = cvService.uploadCV(file, internId, uploaderEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tải lên CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 8. HR initiates CV approval (sets status to ACCEPTING)
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptCV(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            Map<String, Object> result = cvService.acceptCV(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 8.1. HR confirms CV approval (sets status to APPROVED and sends email)
    @PutMapping("/{id}/confirm-approve")
    public ResponseEntity<?> confirmApproveCV(@PathVariable Long id) {
        try {
            Map<String, Object> result = cvService.confirmApproveCV(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 9. HR initiates CV rejection (sets status to REJECTING)
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectCV(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : null;
            Map<String, Object> result = cvService.rejectCV(id, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 9.1. HR confirms CV rejection (sets status to REJECTED and sends email)
    @PutMapping("/{id}/confirm-reject")
    public ResponseEntity<?> confirmRejectCV(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            Map<String, Object> result = cvService.confirmRejectCV(id, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // Legacy endpoint for backward compatibility
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCV(@PathVariable Long id) {
        try {
            Map<String, Object> result = cvService.legacyApproveCV(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 10. Xóa CV
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCV(@PathVariable Long id) {
        try {
            Map<String, Object> result = cvService.deleteCV(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa CV thất bại: " + e.getMessage()
            ));
        }
    }
}