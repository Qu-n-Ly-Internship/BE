package com.example.be.service;

import com.example.be.config.JwtUtil;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public String extractEmailFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.extractEmail(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public Map<String, Object> getMyProfile(String authHeader) {
        String email = extractEmailFromToken(authHeader);
        if (email == null) {
            throw new RuntimeException("Token không hợp lệ!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("role", user.getRole().getName());
        profile.put("status", user.getStatus());

        // Nếu là INTERN hoặc USER, lấy thêm thông tin từ intern_profiles
        if ("INTERN".equals(user.getRole().getName()) || "USER".equals(user.getRole().getName())) {
            profile.putAll(getInternProfileData(user));
        }

        // Nếu là HR hoặc ADMIN, thêm thông tin công ty
        if ("HR".equals(user.getRole().getName()) || "ADMIN".equals(user.getRole().getName())) {
            profile.putAll(getHrAdminData(user.getRole().getName()));
        }

        // Nếu là ADMIN, thêm permissions
        if ("ADMIN".equals(user.getRole().getName())) {
            profile.put("permissions", "Full Access");
        }

        // Nếu là USER (chưa được duyệt), thêm thông tin đăng ký
        if ("USER".equals(user.getRole().getName()) && "PENDING".equals(user.getStatus())) {
            profile.putAll(getPendingUserData());
        }

        return profile;
    }

    private Map<String, Object> getInternProfileData(User user) {
        String sql = """
                SELECT i.intern_id, i.fullname, i.dob, i.major_id, i.year_of_study, i.phone, i.available_from,
                       u.name_uni as university_name,
                       p.title as program_title, p.start_date, p.end_date
                FROM intern_profiles i
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                WHERE i.fullname = ? OR i.phone = ?
                LIMIT 1
                """;

        List<Map<String, Object>> internData = jdbcTemplate.queryForList(sql,
                user.getFullName(),
                user.getEmail().split("@")[0]
        );

        Map<String, Object> result = new HashMap<>();
        if (!internData.isEmpty()) {
            Map<String, Object> intern = internData.get(0);
            result.put("university", intern.get("university_name"));
            result.put("major", intern.get("major_id"));
            result.put("yearOfStudy", intern.get("year_of_study"));
            result.put("phone", intern.get("phone"));
            result.put("startDate", intern.get("start_date"));
            result.put("endDate", intern.get("end_date"));
            result.put("mentorName", "Chưa phân công"); // TODO: Lấy từ DB nếu có
            result.put("availableFrom", intern.get("available_from"));
        } else {
            result.put("university", null);
            result.put("major", null);
            result.put("yearOfStudy", null);
            result.put("phone", null);
            result.put("startDate", null);
            result.put("endDate", null);
            result.put("mentorName", null);
        }

        return result;
    }

    private Map<String, Object> getHrAdminData(String role) {
        Map<String, Object> result = new HashMap<>();
        result.put("department", "IT Department");
        result.put("position", "HR".equals(role) ? "HR Manager" : "System Admin");
        result.put("joinDate", "2020-01-01"); // TODO: Lấy từ DB nếu có
        return result;
    }

    private Map<String, Object> getPendingUserData() {
        Map<String, Object> result = new HashMap<>();
        result.put("appliedDate", java.time.LocalDate.now().toString());
        result.put("expectedStartDate", "2024-09-01"); // TODO: Lấy từ DB nếu có
        return result;
    }

    public Map<String, Object> updateProfile(String authHeader, Map<String, Object> request) {
        String email = extractEmailFromToken(authHeader);
        if (email == null) {
            throw new RuntimeException("Token không hợp lệ!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        updateBasicInfo(user, request);
        updatePassword(user, request);

        User savedUser = userRepository.save(user);

        return Map.of(
                "message", "Cập nhật profile thành công!",
                "user", Map.of(
                        "id", savedUser.getId(),
                        "email", savedUser.getEmail(),
                        "fullName", savedUser.getFullName(),
                        "role", savedUser.getRole().getName(),
                        "status", savedUser.getStatus()
                )
        );
    }

    private void updateBasicInfo(User user, Map<String, Object> request) {
        if (request.containsKey("fullName")) {
            user.setFullName((String) request.get("fullName"));
        }
    }

    private void updatePassword(User user, Map<String, Object> request) {
        if (request.containsKey("password") && request.get("password") != null) {
            String newPassword = (String) request.get("password");
            if (!newPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
        }
    }
}
