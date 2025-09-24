package com.example.be.controller;

import com.example.be.entity.Permission;
import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.entity.UserPermission;
import com.example.be.repository.PermissionRepository;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import com.example.be.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final UserPermissionRepository userPermissionRepository;

    // SCRUM-101: Quản lý quyền của vai trò

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
                "description", p.getDescription() != null ? p.getDescription() : ""
        )).toList();

        return ResponseEntity.ok(Map.of(
                "role",role.getName(),
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

            return ResponseEntity.ok(Map.of("message", "Cập nhật quyền cho role thành công!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // SCRUM-102: Quản lý cấp quyền cho user

    // Lấy permissions của một user (bao gồm từ role + riêng)
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại!"));
        }

        User user = userOpt.get();

        // Permissions từ role
        Set<String> rolePermissions = user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        // Permissions riêng của user
        var userPermissions = userPermissionRepository.findByUserId(userId);
        Map<String, Boolean> userSpecificPermissions = userPermissions.stream()
                .collect(Collectors.toMap(
                        up -> up.getPermission().getName(),
                        UserPermission::getGranted
                ));

        return ResponseEntity.ok(Map.of(
                "user", Map.of("id", user.getId(), "fullName", user.getFullName(), "role", user.getRole().getName()),
                "rolePermissions", rolePermissions,
                "userSpecificPermissions", userSpecificPermissions
        ));
    }

    // Cấp/Thu hồi quyền riêng cho user
    @PutMapping("/user/{userId}")
    public ResponseEntity<?> updateUserPermissions(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {
        try {
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User không tồn tại!"));
            }

            User user = userOpt.get();
            Long permissionId = Long.valueOf((Integer) request.get("permissionId"));
            Boolean granted = (Boolean) request.get("granted");

            var permissionOpt = permissionRepository.findById(permissionId);
            if (permissionOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Permission không tồn tại!"));
            }

            Permission permission = permissionOpt.get();

            // Tìm hoặc tạo UserPermission
            var userPermissionOpt = userPermissionRepository.findByUserIdAndPermissionId(userId, permissionId);
            UserPermission userPermission;

            if (userPermissionOpt.isPresent()) {
                userPermission = userPermissionOpt.get();
                userPermission.setGranted(granted);
            } else {
                userPermission = UserPermission.builder()
                        .user(user)
                        .permission(permission)
                        .granted(granted)
                        .build();
            }

            userPermissionRepository.save(userPermission);

            String action = granted ? "cấp" : "thu hồi";
            return ResponseEntity.ok(Map.of("message",
                    "Đã " + action + " quyền '" + permission.getName() + "' cho user " + user.getFullName()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Tạo permission mới (cho admin)
    @PostMapping("")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, String> request) {
        try {
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
}