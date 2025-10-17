package com.example.be.service;

import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> createUser(Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");
            String roleName = request.get("role") != null ? request.get("role").toUpperCase() : "INTERN";

            if (userRepository.findByEmail(email).isPresent()) {
                return Map.of("message", "Email đã tồn tại!");
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                String emailPrefix = email.split("@")[0];
                fullName = emailPrefix.substring(0, 1).toUpperCase() + emailPrefix.substring(1);
            }

            String username = email.split("@")[0];
            String finalUsername = username;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = username + suffix;
                suffix++;
            }

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUsername(finalUsername);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setStatus("ACTIVE");
            user.setAuthProvider("LOCAL");

            User savedUser = userRepository.save(user);

            return Map.of(
                "id", savedUser.getId(),
                "fullName", savedUser.getFullName(),
                "email", savedUser.getEmail(),
                "role", savedUser.getRole().getName(),
                "status", savedUser.getStatus()
            );
        } catch (Exception e) {
            throw new RuntimeException("Tạo tài khoản thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> getUsers(String query, String role, String status,
            boolean excludeInternProfiles, int page, int size) {
        try {
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder("""
                SELECT u.user_id as id, u.fullname as fullName, u.email, u.status,
                       r.name as role
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE 1=1
                """);

            if (query != null && !query.isEmpty()) {
                sql.append(" AND (u.fullname LIKE ? OR u.email LIKE ?)");
                String pattern = "%" + query + "%";
                params.add(pattern);
                params.add(pattern);
            }

            if (role != null && !role.isEmpty()) {
                sql.append(" AND r.name = ?");
                params.add(role);
            }

            if (status != null && !status.isEmpty()) {
                sql.append(" AND u.status = ?");
                params.add(status);
            }

            if (excludeInternProfiles && "INTERN".equalsIgnoreCase(role)) {
                sql.append(" AND u.email NOT IN (SELECT email FROM intern_profiles)");
            }

            sql.append(" ORDER BY u.fullname LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            return Map.of(
                "content", users,
                "total", users.size(),
                "totalPages", 1,
                "totalUsers", userRepository.count()
            );
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải danh sách: " + e.getMessage());
        }
    }

    public Map<String, Object> updateUser(Long id, Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String username = user.getEmail().split("@")[0];
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.findByUsername(finalUsername).isPresent()) {
                    finalUsername = username + suffix;
                    suffix++;
                }
                user.setUsername(finalUsername);
            }

            if (request.containsKey("fullName")) {
                user.setFullName(request.get("fullName"));
            }
            if (request.containsKey("role")) {
                String roleName = request.get("role").toUpperCase();
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));
                user.setRole(role);
            }
            if (request.containsKey("status")) {
                user.setStatus(request.get("status"));
            }

            userRepository.save(user);
            return Map.of("message", "Cập nhật thành công");
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật thất bại: " + e.getMessage());
        }
    }

    public void deleteUser(Long id) {
        try {
            if (!userRepository.existsById(id)) {
                throw new RuntimeException("Không tìm thấy user!");
            }
            userRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Xóa thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> approveUser(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            ensureValidUsername(user);
            user.setStatus("ACTIVE");
            userRepository.save(user);

            return Map.of("message", "User " + user.getEmail() + " đã được duyệt!");
        } catch (Exception e) {
            throw new RuntimeException("Duyệt thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> disableUser(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            ensureValidUsername(user);
            user.setStatus("INACTIVE");
            userRepository.save(user);

            return Map.of("message", "User " + user.getEmail() + " đã bị khóa!");
        } catch (Exception e) {
            throw new RuntimeException("Khóa thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> resetUserToDefault(Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            ensureValidUsername(user);

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER không tồn tại!"));
            user.setRole(userRole);
            user.setStatus("PENDING");

            User savedUser = userRepository.save(user);

            return Map.of(
                "message", "Đã reset user " + user.getEmail() + " về role USER và status PENDING!",
                "user", Map.of(
                    "id", savedUser.getId(),
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole().getName(),
                    "status", savedUser.getStatus()
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Reset thất bại: " + e.getMessage());
        }
    }

    private void ensureValidUsername(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            String username = user.getEmail().split("@")[0];
            String finalUsername = username;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = username + suffix;
                suffix++;
            }
            user.setUsername(finalUsername);
        }
    }
}
