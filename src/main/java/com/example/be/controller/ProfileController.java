package com.example.be.controller;

import com.example.be.config.JwtUtil;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    // Lấy profile của user hiện tại
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Lấy email từ token
            String email = extractEmailFromToken(authHeader);
            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token không hợp lệ!"));
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

                if (!internData.isEmpty()) {
                    Map<String, Object> intern = internData.get(0);
                    profile.put("university", intern.get("university_name"));
                    profile.put("major", intern.get("major_id"));
                    profile.put("yearOfStudy", intern.get("year_of_study"));
                    profile.put("phone", intern.get("phone"));
                    profile.put("startDate", intern.get("start_date"));
                    profile.put("endDate", intern.get("end_date"));
                    profile.put("mentorName", "Chưa phân công"); // TODO: Lấy từ DB nếu có
                    profile.put("availableFrom", intern.get("available_from"));
                } else {
                    // Nếu chưa có profile intern, trả về giá trị mặc định
                    profile.put("university", null);
                    profile.put("major", null);
                    profile.put("yearOfStudy", null);
                    profile.put("phone", null);
                    profile.put("startDate", null);
                    profile.put("endDate", null);
                    profile.put("mentorName", null);
                }
            }

            // Nếu là HR hoặc ADMIN, thêm thông tin công ty
            if ("HR".equals(user.getRole().getName()) || "ADMIN".equals(user.getRole().getName())) {
                profile.put("department", "IT Department");
                profile.put("position", "HR".equals(user.getRole().getName()) ? "HR Manager" : "System Admin");
                profile.put("joinDate", "2020-01-01"); // TODO: Lấy từ DB nếu có
            }

            // Nếu là ADMIN, thêm permissions
            if ("ADMIN".equals(user.getRole().getName())) {
                profile.put("permissions", "Full Access");
            }

            // Nếu là USER (chưa được duyệt), thêm thông tin đăng ký
            if ("USER".equals(user.getRole().getName()) && "PENDING".equals(user.getStatus())) {
                profile.put("appliedDate", java.time.LocalDate.now().toString());
                profile.put("expectedStartDate", "2024-09-01"); // TODO: Lấy từ DB nếu có
            }

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Cập nhật profile
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> request
    ) {
        try {
            // Lấy email từ token
            String email = extractEmailFromToken(authHeader);
            if (email == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Token không hợp lệ!"));
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

            // Cập nhật thông tin cơ bản
            if (request.containsKey("fullName")) {
                user.setFullName((String) request.get("fullName"));
            }

            // Cập nhật password nếu có
            if (request.containsKey("password") && request.get("password") != null) {
                String newPassword = (String) request.get("password");
                if (!newPassword.trim().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                }
            }

            userRepository.save(user);

            // TODO: Nếu cần cập nhật thông tin intern_profiles, thêm logic ở đây

            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật profile thành công!",
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName(),
                            "role", user.getRole().getName(),
                            "status", user.getStatus()
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // Helper method để extract email từ JWT token
    private String extractEmailFromToken(String authHeader) {
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
}
