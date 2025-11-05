package com.example.be.controller;

import com.example.be.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    // ==================== QUẢN LÝ PERMISSIONS ====================

    @GetMapping("")
    public ResponseEntity<?> getAllPermissions() {
        try {
            Map<String, Object> result = permissionService.getAllPermissions();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách quyền: " + e.getMessage()
            ));
        }
    }

    @PostMapping("")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = permissionService.createPermission(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo quyền mới: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = permissionService.updatePermission(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật quyền: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermission(@PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa quyền thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi xóa quyền: " + e.getMessage()
            ));
        }
    }

    // ==================== QUẢN LÝ QUYỀN CỦA ROLE ====================

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        try {
            var roles = permissionService.getAllRoles();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", roles
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách roles: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long roleId) {
        try {
            Map<String, Object> result = permissionService.getRoleWithPermissions(roleId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/role/{roleId}")
    public ResponseEntity<?> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            var permissionIds = (java.util.List<Integer>) request.get("permissionIds");
            Map<String, Object> result = permissionService.updateRolePermissionsByIds(roleId, permissionIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // ==================== THỐNG KÊ ====================

    @GetMapping("/stats")
    public ResponseEntity<?> getPermissionStats() {
        try {
            Map<String, Object> stats = permissionService.getPermissionStats();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }
}