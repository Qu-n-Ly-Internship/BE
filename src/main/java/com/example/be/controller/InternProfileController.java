package com.example.be.controller;

import com.example.be.service.InternProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/intern-profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class InternProfileController {

    private final InternProfileService internProfileService;

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // 1. Lấy danh sách intern profiles với filter
    @GetMapping("")
    public ResponseEntity<?> getAllProfiles(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "school", required = false) String school,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size) {
        try {
            Map<String, Object> result = internProfileService.getAllProfiles(query, school, major, status, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải danh sách: " + e.getMessage()));
        }
    }

    // 2. Tạo intern profile mới
    @PostMapping("")
    public ResponseEntity<?> createProfile(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = internProfileService.createProfile(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thêm thất bại: " + e.getMessage()));
        }
    }

    // 3. Cập nhật intern profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = internProfileService.updateProfile(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // 4. Xóa intern profile
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            internProfileService.deleteProfile(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa thất bại: " + e.getMessage()));
        }
    }

    // 5. Thống kê số lượng intern theo trạng thái
    @GetMapping("/stats/status")
    public ResponseEntity<?> getInternStatusStats() {
        try {
            String sql = """
                    SELECT status, COUNT(*) AS count
                    FROM intern_profiles
                    GROUP BY status
                    """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi thống kê trạng thái thực tập sinh: " + e.getMessage()));
        }
    }
}