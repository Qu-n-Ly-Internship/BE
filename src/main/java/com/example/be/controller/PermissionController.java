package com.example.be.controller;

import com.example.be.entity.Permission;
import com.example.be.entity.Role;
import com.example.be.repository.PermissionRepository;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // ==================== QUẢN LÝ PERMISSIONS ====================

    // Lấy tất cả permissions
    @GetMapping("")
    public ResponseEntity<?> getAllPermissions() {
        var permissions = permissionRepository.findAll();
        return ResponseEntity.ok(permissions.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : "",
                "module", p.getModule() != null ? p.getModule() : ""
        )).toList());
    }

    // Tạo permission mới (cho admin)
    @PostMapping("")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, String> request) {
        try {
            // Check trùng tên
            if (permissionRepository.findByName(request.get("name")).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Permission name đã tồn tại!"));
            }

            Permission permission = Permission.builder()
                    .name(request.get("name"))
                    .description(request.get("description"))
                    .module(request.get("module"))
                    .build();

            permissionRepository.save(permission);
            return ResponseEntity.ok(Map.of("message", "Tạo permission thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Cập nhật permission
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Permission permission = permissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Permission không tồn tại!"));

            if (request.containsKey("name")) {
                permission.setName(request.get("name"));
            }
            if (request.containsKey("description")) {
                permission.setDescription(request.get("description"));
            }
            if (request.containsKey("module")) {
                permission.setModule(request.get("module"));
            }

            permissionRepository.save(permission);
            return ResponseEntity.ok(Map.of("message", "Cập nhật permission thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Xóa permission
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermission(@PathVariable Long id) {
        try {
            if (!permissionRepository.existsById(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Permission không tồn tại!"));
            }

            permissionRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa permission thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // ==================== QUẢN LÝ QUYỀN CỦA ROLE ====================

    // Lấy tất cả roles với permissions
    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        var roles = roleRepository.findAll();
        return ResponseEntity.ok(roles.stream().map(role -> Map.of(
                "id", role.getId(),
                "name", role.getName(),
                "description", role.getDescription() != null ? role.getDescription() : "",
                "permissionCount", role.getPermissions().size()
        )).toList());
    }

    // Lấy permissions của một role
    @GetMapping("/role/{roleId}")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long roleId) {
        var roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role không tồn tại!"));
        }

        Role role = roleOpt.get();
        var permissions = role.getPermissions().stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : "",
                "module", p.getModule() != null ? p.getModule() : ""
        )).toList();

        return ResponseEntity.ok(Map.of(
                "role", Map.of(
                        "id", role.getId(),
                        "name", role.getName(),
                        "description", role.getDescription()
                ),
                "permissions", permissions
        ));
    }

    // Cập nhật permissions cho role
    @PutMapping("/role/{roleId}")
    public ResponseEntity<?> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody Map<String, Object> request) {
        try {
            var roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Role không tồn tại!"));
            }

            Role role = roleOpt.get();
            @SuppressWarnings("unchecked")
            var permissionIds = (java.util.List<Integer>) request.get("permissionIds");

            // Lấy permissions từ IDs
            Set<Permission> permissions = permissionIds.stream()
                    .map(id -> permissionRepository.findById(Long.valueOf(id)))
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .collect(Collectors.toSet());

            role.setPermissions(permissions);
            roleRepository.save(role);

            return ResponseEntity.ok(Map.of("message", "Cập nhật quyền cho role " + role.getName() + " thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // ==================== THỐNG KÊ ====================

    // Thống kê permissions theo module
    @GetMapping("/stats")
    public ResponseEntity<?> getPermissionStats() {
        var permissions = permissionRepository.findAll();

        // Group by module
        var moduleStats = permissions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getModule() != null ? p.getModule() : "OTHER",
                        Collectors.counting()
                ));

        // Total permissions
        long totalPermissions = permissions.size();

        // Total roles
        long totalRoles = roleRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalPermissions", totalPermissions,
                "totalRoles", totalRoles,
                "moduleStats", moduleStats
        ));
    }
}