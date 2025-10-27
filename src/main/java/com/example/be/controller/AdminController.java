package com.example.be.controller;

import com.example.be.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // Tạo user mới với email và password
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> response = adminService.createUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Tạo tài khoản thất bại: " + e.getMessage()));
        }
    }

    // Lấy danh sách users với filter và phân trang
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "role", defaultValue = "") String role,
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "excludeInternProfiles", defaultValue = "false") boolean excludeInternProfiles,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            Map<String, Object> result = adminService.getUsers(query, role, status, excludeInternProfiles, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không thể tải danh sách: " + e.getMessage()));
        }
    }

    // Cập nhật thông tin user
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = adminService.updateUser(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // Xóa user
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Xóa thất bại: " + e.getMessage()));
        }
    }

    // Duyệt user
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        try {
            Map<String, Object> result = adminService.approveUser(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Duyệt thất bại: " + e.getMessage()));
        }
    }

    // Khóa user
    @PutMapping("/disable/{id}")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            Map<String, Object> result = adminService.disableUser(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Khóa thất bại: " + e.getMessage()));
        }
    }

    // Reset user về role USER và status PENDING (dùng khi user bị gán sai role)
    @PutMapping("/reset/{id}")
    public ResponseEntity<?> resetUserToDefault(@PathVariable Long id) {
        try {
            Map<String, Object> result = adminService.resetUserToDefault(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Reset thất bại: " + e.getMessage()));
        }
    }

    // Thống kê số lượng user theo role
    @GetMapping("/stats/roles")
    public ResponseEntity<?> getUserRoleStats() {
        try {
            List<Map<String, Object>> result = adminService.getUserRoleStats();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi thống kê roles: " + e.getMessage()));
        }
    }
}
