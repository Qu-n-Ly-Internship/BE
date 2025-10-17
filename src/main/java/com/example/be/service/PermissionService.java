package com.example.be.service;

import com.example.be.entity.Permission;
import com.example.be.entity.Role;
import com.example.be.repository.PermissionRepository;
import com.example.be.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public Map<String, Object> getAllPermissions() {
        try {
            List<Permission> permissions = permissionRepository.findAll();
            List<Map<String, Object>> permissionList = permissions.stream()
                .map(p -> {
                    Map<String, Object> permMap = new HashMap<>();
                    permMap.put("permission_id", p.getId());
                    permMap.put("permission_name", p.getName());
                    permMap.put("description", p.getDescription() != null ? p.getDescription() : "");
                    permMap.put("module", p.getModule() != null ? p.getModule() : "");
                    permMap.put("roles", String.join(",",
                        p.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList())));
                    return permMap;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", permissionList);
            response.put("total", permissionList.size());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách quyền: " + e.getMessage());
        }
    }

    public Map<String, Object> getRolePermissions(String roleName) {
        try {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

            List<Map<String, Object>> permissions = role.getPermissions().stream()
                .map(p -> {
                    Map<String, Object> permMap = new HashMap<>();
                    permMap.put("permission_id", p.getId());
                    permMap.put("name", p.getName());
                    permMap.put("description", p.getDescription() != null ? p.getDescription() : "");
                    return permMap;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", permissions);
            response.put("total", permissions.size());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy quyền của role: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateRolePermissions(String roleName, List<String> permissionNames) {
        try {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

            Set<Permission> newPermissions = permissionNames.stream()
                .map(name -> permissionRepository.findByName(name)
                    .orElseThrow(() -> new RuntimeException("Permission không tồn tại: " + name)))
                .collect(Collectors.toSet());

            role.setPermissions(newPermissions);
            roleRepository.save(role);

            return Map.of(
                "success", true,
                "message", "Cập nhật quyền thành công cho role: " + roleName,
                "permissions", permissionNames
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật quyền: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> createPermission(Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String module = request.get("module");

            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên quyền không được để trống");
            }

            String normalizedName = name.toUpperCase().trim();
            if (permissionRepository.findByName(normalizedName).isPresent()) {
                throw new IllegalArgumentException("Quyền đã tồn tại: " + normalizedName);
            }

            Permission permission = Permission.builder()
                .name(normalizedName)
                .description(description)
                .module(module)
                .build();

            permission = permissionRepository.save(permission);

            return Map.of(
                "success", true,
                "message", "Tạo quyền mới thành công",
                "data", Map.of(
                    "id", permission.getId(),
                    "name", permission.getName(),
                    "description", permission.getDescription(),
                    "module", permission.getModule()
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo quyền mới: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updatePermission(Long id, Map<String, String> request) {
        try {
            Permission permission = permissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Permission không tồn tại!"));

            if (request.containsKey("name")) {
                String newName = request.get("name").toUpperCase().trim();
                if (!newName.equals(permission.getName()) &&
                    permissionRepository.findByName(newName).isPresent()) {
                    throw new IllegalArgumentException("Tên quyền đã tồn tại: " + newName);
                }
                permission.setName(newName);
            }

            if (request.containsKey("description")) {
                permission.setDescription(request.get("description"));
            }
            if (request.containsKey("module")) {
                permission.setModule(request.get("module"));
            }

            permission = permissionRepository.save(permission);

            return Map.of(
                "success", true,
                "message", "Cập nhật permission thành công!",
                "data", Map.of(
                    "id", permission.getId(),
                    "name", permission.getName(),
                    "description", permission.getDescription(),
                    "module", permission.getModule()
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật permission: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> deletePermission(Long id) {
        try {
            Permission permission = permissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Permission không tồn tại!"));

            // Clear associations from both sides
            for (Role role : permission.getRoles()) {
                role.getPermissions().remove(permission);
                roleRepository.save(role);
            }
            permission.getRoles().clear();
            permissionRepository.delete(permission);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa permission thành công!");
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa permission: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", role.getId());
                    roleMap.put("name", role.getName());
                    roleMap.put("description", role.getDescription() != null ? role.getDescription() : "");
                    roleMap.put("permissionCount", role.getPermissions().size());
                    return roleMap;
                })
                .toList();
    }

    public Map<String, Object> getRoleWithPermissions(Long roleId) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

            List<Map<String, Object>> permissions = role.getPermissions().stream()
                .map(p -> {
                    Map<String, Object> permMap = new HashMap<>();
                    permMap.put("id", p.getId());
                    permMap.put("name", p.getName());
                    permMap.put("description", p.getDescription() != null ? p.getDescription() : "");
                    permMap.put("module", p.getModule() != null ? p.getModule() : "");
                    return permMap;
                })
                .toList();

            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("name", role.getName());
            roleMap.put("description", role.getDescription());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "role", roleMap,
                "permissions", permissions
            ));
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thông tin role: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateRolePermissionsByIds(Long roleId, List<Integer> permissionIds) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

            Set<Permission> permissions = permissionIds.stream()
                    .map(id -> permissionRepository.findById(Long.valueOf(id))
                        .orElseThrow(() -> new RuntimeException("Permission không tồn tại: " + id)))
                    .collect(Collectors.toSet());

            role.setPermissions(permissions);
            roleRepository.save(role);

            return Map.of(
                "success", true,
                "message", "Cập nhật quyền cho role " + role.getName() + " thành công!",
                "data", Map.of(
                    "roleId", role.getId(),
                    "permissionCount", permissions.size()
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật quyền: " + e.getMessage());
        }
    }

    public Map<String, Object> getPermissionStats() {
        try {
            var permissions = permissionRepository.findAll();
            var roles = roleRepository.findAll();

            var moduleStats = permissions.stream()
                    .collect(Collectors.groupingBy(
                        p -> p.getModule() != null ? p.getModule() : "OTHER",
                        Collectors.counting()
                    ));

            var roleStats = roles.stream()
                    .collect(Collectors.toMap(
                        Role::getName,
                        role -> role.getPermissions().size()
                    ));

            return Map.of(
                "success", true,
                "data", Map.of(
                    "totalPermissions", permissions.size(),
                    "totalRoles", roles.size(),
                    "moduleStats", moduleStats,
                    "roleStats", roleStats
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }
}
