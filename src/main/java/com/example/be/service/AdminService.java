package com.example.be.service;

import com.example.be.entity.*;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private HrRepository hrRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> createUser(Map<String, String> request) {
        try {
            // ====== Lấy dữ liệu từ request ======
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");
            String roleName = request.getOrDefault("role", "INTERN").toUpperCase();

            // ====== Kiểm tra email trùng ======
            if (userRepository.findByEmail(email).isPresent()) {
                return Map.of("message", "Email đã tồn tại!");
            }

            // ====== Xử lý tên đầy đủ nếu chưa có ======
            if (fullName == null || fullName.trim().isEmpty()) {
                String emailPrefix = email.split("@")[0];
                fullName = emailPrefix.substring(0, 1).toUpperCase() + emailPrefix.substring(1);
            }

            // ====== Sinh username không trùng ======
            String username = email.split("@")[0];
            String finalUsername = username;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = username + suffix;
                suffix++;
            }

            // ====== Lấy role ======
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

            // ====== Khởi tạo User ======
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUsername(finalUsername);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setStatus("ACTIVE");
            user.setAuthProvider("LOCAL");

            // ====== Lưu User ======
            User savedUser = userRepository.save(user);

            // ====== Tạo bản ghi phụ thuộc theo role ======
            createRoleLinkedRecord(savedUser, roleName);

            // ====== Trả kết quả ======
            return Map.of(
                    "id", savedUser.getId(),
                    "fullName", savedUser.getFullName(),
                    "email", savedUser.getEmail(),
                    "username", savedUser.getUsername(),
                    "role", savedUser.getRole().getName(),
                    "status", savedUser.getStatus(),
                    "message", "Tạo tài khoản thành công!");

        } catch (Exception e) {
            throw new RuntimeException("Tạo tài khoản thất bại: " + e.getMessage(), e);
        }
    }

    private void createRoleLinkedRecord(User savedUser, String roleName) {
        switch (roleName) {
            case "HR" -> {
                Hr hr = new Hr();
                hr.setUser(savedUser);
                hr.setFullname(savedUser.getFullName());
                hrRepository.save(hr);
            }
            case "MENTOR" -> {
                Mentors mentor = new Mentors();
                mentor.setUser(savedUser);
                mentor.setFullName(savedUser.getFullName());
                mentorRepository.save(mentor);
            }
            case "INTERN" -> {
                InternProfile intern = new InternProfile();
                intern.setUser(savedUser);
                intern.setFullName(savedUser.getFullName());
                intern.setEmail(savedUser.getEmail());
                internProfileRepository.save(intern);
            }
            default -> {

            }
        }
    }

    public Map<String, Object> getUsers(String query, String role, String status,
            boolean excludeInternProfiles, int page, int size) {
        try {
            List<Object> params = new ArrayList<>();
            List<Object> countParams = new ArrayList<>();

            String baseSql = """
                        FROM users u
                        JOIN roles r ON u.role_id = r.role_id
                        WHERE 1=1
                    """;

            StringBuilder condition = new StringBuilder();

            if (query != null && !query.isEmpty()) {
                condition.append(" AND (u.fullname LIKE ? OR u.email LIKE ?)");
                String pattern = "%" + query + "%";
                params.add(pattern);
                params.add(pattern);
                countParams.add(pattern);
                countParams.add(pattern);
            }

            if (role != null && !role.isEmpty()) {
                condition.append(" AND r.name = ?");
                params.add(role);
                countParams.add(role);
            }

            if (status != null && !status.isEmpty()) {
                condition.append(" AND u.status = ?");
                params.add(status);
                countParams.add(status);
            }

            if (excludeInternProfiles && "INTERN".equalsIgnoreCase(role)) {
                condition.append(" AND u.email NOT IN (SELECT email FROM intern_profiles)");
            }

            // --- Truy vấn dữ liệu phân trang ---
            String sql = """
                        SELECT u.user_id AS id, u.fullname AS fullName, u.email, u.status, r.name AS role
                    """ + baseSql + condition + " ORDER BY u.fullname LIMIT ? OFFSET ?";

            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, params.toArray());

            // --- Truy vấn tổng số bản ghi ---
            String countSql = "SELECT COUNT(*) " + baseSql + condition;
            int total = jdbcTemplate.queryForObject(countSql, countParams.toArray(), Integer.class);
            int totalPages = (int) Math.ceil((double) total / size);

            return Map.of(
                    "content", users,
                    "total", total,
                    "totalPages", totalPages,
                    "totalUsers", total);
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
                            "status", savedUser.getStatus()));
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

    // Lấy thống kê số lượng user theo role
    public List<Map<String, Object>> getUserRoleStats() {
        String sql = """
                    SELECT r.name AS role, COUNT(u.user_id) AS count
                    FROM users u
                    LEFT JOIN roles r ON u.role_id = r.role_id
                    GROUP BY r.name
                """;
        return jdbcTemplate.queryForList(sql);
    }

}
