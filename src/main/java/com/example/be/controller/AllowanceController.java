package com.example.be.controller;

import com.example.be.service.AllowanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/allowances")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AllowanceController {

    private final AllowanceService allowanceService;

    // Lấy danh sách phụ cấp với filter
    @GetMapping("")
    public ResponseEntity<?> getAllAllowances(
            @RequestParam(value = "internId", required = false) String internId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Map<String, Object> result = allowanceService.getAllAllowances(internId, startDate, endDate, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy chi tiết một phụ cấp
    @GetMapping("/{id}")
    public ResponseEntity<?> getAllowanceById(@PathVariable Long id) {
        try {
            Map<String, Object> result = allowanceService.getAllowanceById(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Tạo phụ cấp mới
    @PostMapping("")
    public ResponseEntity<?> createAllowance(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = allowanceService.createAllowance(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Cập nhật phụ cấp
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAllowance(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = allowanceService.updateAllowance(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Xóa phụ cấp
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAllowance(@PathVariable Long id) {
        try {
            Map<String, Object> result = allowanceService.deleteAllowance(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Duyệt/thanh toán phụ cấp
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveAllowance(@PathVariable Long id) {
        try {
            Map<String, Object> result = allowanceService.approveAllowance(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy thống kê phụ cấp theo intern
    @GetMapping("/stats/by-intern/{internId}")
    public ResponseEntity<?> getAllowanceStatsByIntern(@PathVariable Long internId) {
        try {
            Map<String, Object> result = allowanceService.getAllowanceStatsByIntern(internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy danh sách phụ cấp chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAllowances(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Map<String, Object> result = allowanceService.getPendingAllowances(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy danh sách phụ cấp đã duyệt
    @GetMapping("/approved")
    public ResponseEntity<?> getApprovedAllowances(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Map<String, Object> result = allowanceService.getApprovedAllowances(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy dashboard thống kê
    @GetMapping("/dashboard")
    public ResponseEntity<?> getAllowanceDashboard() {
        try {
            Map<String, Object> result = allowanceService.getAllowanceDashboard();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Duyệt nhiều phụ cấp cùng lúc
    @PostMapping("/approve-multiple")
    public ResponseEntity<?> approveMultiple(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> allowanceIds = (List<Long>) request.get("allowanceIds");
            Map<String, Object> result = allowanceService.approveMultiple(allowanceIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Xuất báo cáo phụ cấp theo tháng
    @GetMapping("/report/monthly")
    public ResponseEntity<?> getMonthlyReport(@RequestParam String month) {
        try {
            Map<String, Object> result = allowanceService.getMonthlyReport(month);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
}