package com.example.be.service;

import com.example.be.config.JwtUtil;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

        // Nếu là HR, ADMIN hoặc MENTOR, thêm thông tin
        if ("HR".equals(user.getRole().getName()) ||
                "ADMIN".equals(user.getRole().getName()) ||
                "MENTOR".equals(user.getRole().getName())) {
            profile.putAll(getHrAdminMentorData(user.getRole().getName(), user.getId()));
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
        // ✅ Query mới: Lấy đầy đủ thông tin từ intern_profiles và mentor từ intern_programs
        String sql = """
                SELECT 
                    ip.intern_id,
                    ip.fullname,
                    ip.email,
                    ip.phone,
                    ip.dob,
                    ip.year_of_study,
                    ip.status,
                    ip.available_from,
                    ip.end_date,
                    u.name_uni as university_name,
                    m.name_major as major_name,
                    prog.title as program_title,
                    mentor.fullname as mentor_name,
                    mentor.mentor_id
                FROM intern_profiles ip
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                LEFT JOIN majors m ON ip.major_id = m.major_id
                LEFT JOIN intern_programs prog ON ip.program_id = prog.program_id
                LEFT JOIN mentors mentor ON prog.mentor_id = mentor.mentor_id
                WHERE ip.user_id = ?
                LIMIT 1
                """;

        List<Map<String, Object>> internData = jdbcTemplate.queryForList(sql, user.getId());

        Map<String, Object> result = new HashMap<>();
        if (!internData.isEmpty()) {
            Map<String, Object> intern = internData.get(0);

            // Thông tin cơ bản
            result.put("internId", intern.get("intern_id"));
            result.put("phone", intern.get("phone"));
            result.put("dob", intern.get("dob"));
            result.put("yearOfStudy", intern.get("year_of_study"));

            // Trường và ngành (từ intern_profiles)
            result.put("university", intern.get("university_name"));
            result.put("major", intern.get("major_name"));

            // Thời gian thực tập (từ intern_profiles)
            result.put("startDate", intern.get("available_from"));
            result.put("endDate", intern.get("end_date"));

            // Mentor (từ intern_programs thông qua program_id)
            String mentorName = (String) intern.get("mentor_name");
            result.put("mentorName", mentorName != null ? mentorName : "Chưa phân công");
            result.put("mentorId", intern.get("mentor_id"));

            // Program/Project
            result.put("programTitle", intern.get("program_title"));

            // Status
            result.put("internStatus", intern.get("status"));

        } else {
            // Nếu chưa có profile intern, trả về giá trị mặc định
            result.put("internId", null);
            result.put("university", null);
            result.put("major", null);
            result.put("yearOfStudy", null);
            result.put("phone", null);
            result.put("dob", null);
            result.put("startDate", null);
            result.put("endDate", null);
            result.put("mentorName", "Chưa phân công");
            result.put("mentorId", null);
            result.put("programTitle", null);
            result.put("internStatus", null);
        }

        return result;
    }

    private Map<String, Object> getHrAdminMentorData(String role, Long userId) {
        Map<String, Object> result = new HashMap<>();

        if ("HR".equals(role)) {
            // ✅ HR: Chỉ hiển thị position
            result.put("position", "HR Manager");
        } else if ("ADMIN".equals(role)) {
            // ✅ ADMIN: Hiển thị position
            result.put("position", "System Admin");
        } else if ("MENTOR".equals(role)) {
            // ✅ MENTOR: Lấy thông tin phòng ban từ bảng mentors và department
            String mentorSql = """
                    SELECT m.mentor_id, d.name_department
                    FROM mentors m
                    LEFT JOIN department d ON m.department_id = d.department_id
                    WHERE m.user_id = ?
                    LIMIT 1
                    """;

            try {
                List<Map<String, Object>> mentorData = jdbcTemplate.queryForList(mentorSql, userId);

                if (!mentorData.isEmpty()) {
                    Map<String, Object> mentor = mentorData.get(0);
                    result.put("department", mentor.get("name_department") != null
                            ? mentor.get("name_department")
                            : "Chưa phân công");
                    result.put("position", "Mentor");
                } else {
                    result.put("department", "Chưa phân công");
                    result.put("position", "Mentor");
                }
            } catch (Exception e) {
                result.put("department", "Chưa phân công");
                result.put("position", "Mentor");
            }
        }

        return result;
    }

    private Map<String, Object> getPendingUserData() {
        Map<String, Object> result = new HashMap<>();
        result.put("appliedDate", java.time.LocalDate.now().toString());
        result.put("expectedStartDate", "2024-09-01");
        return result;
    }

    public Map<String, Object> updateProfile(String authHeader, Map<String, Object> request) {
        String email = extractEmailFromToken(authHeader);
        if (email == null) {
            throw new RuntimeException("Token không hợp lệ!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        // ✅ Cập nhật thông tin cơ bản trong bảng users
        updateBasicInfo(user, request);
        updatePassword(user, request);
        User savedUser = userRepository.save(user);

        // ✅ Nếu là INTERN hoặc USER, cập nhật thêm thông tin trong intern_profiles
        if ("INTERN".equals(user.getRole().getName()) || "USER".equals(user.getRole().getName())) {
            updateInternProfileInfo(user.getId(), request);
        }

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

    /**
     * ✅ Cập nhật thông tin trong bảng intern_profiles
     */
    private void updateInternProfileInfo(Long userId, Map<String, Object> request) {
        try {
            // Kiểm tra xem user có intern_profile không
            String checkSql = "SELECT intern_id FROM intern_profiles WHERE user_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, userId);

            if (result.isEmpty()) {
                System.out.println("⚠️ User không có intern_profile, bỏ qua cập nhật");
                return;
            }

            Long internId = ((Number) result.get(0).get("intern_id")).longValue();

            // Build dynamic UPDATE query
            StringBuilder updateSql = new StringBuilder("UPDATE intern_profiles SET ");
            List<Object> params = new ArrayList<>();
            boolean hasUpdates = false;

            // ✅ Cập nhật fullname
            if (request.containsKey("fullName")) {
                updateSql.append("fullname = ?, ");
                params.add(request.get("fullName"));
                hasUpdates = true;
            }

            // ✅ Cập nhật email
            if (request.containsKey("email")) {
                updateSql.append("email = ?, ");
                params.add(request.get("email"));
                hasUpdates = true;
            }

            // ✅ Cập nhật phone
            if (request.containsKey("phone")) {
                updateSql.append("phone = ?, ");
                params.add(request.get("phone"));
                hasUpdates = true;
            }

            // ✅ Cập nhật university (tìm hoặc tạo uni_id)
            if (request.containsKey("university") && request.get("university") != null) {
                String universityName = request.get("university").toString();
                Integer uniId = getOrCreateUniversity(universityName);
                updateSql.append("uni_id = ?, ");
                params.add(uniId);
                hasUpdates = true;
            }

            // ✅ Cập nhật major (tìm hoặc tạo major_id)
            if (request.containsKey("major") && request.get("major") != null) {
                String majorName = request.get("major").toString();
                Integer majorId = getOrCreateMajor(majorName);
                updateSql.append("major_id = ?, ");
                params.add(majorId);
                hasUpdates = true;
            }

            if (!hasUpdates) {
                return;
            }

            // Remove trailing comma and add WHERE clause
            updateSql.setLength(updateSql.length() - 2);
            updateSql.append(" WHERE intern_id = ?");
            params.add(internId);

            jdbcTemplate.update(updateSql.toString(), params.toArray());
            System.out.println("✅ Updated intern_profile for intern_id: " + internId);

        } catch (Exception e) {
            System.err.println("❌ Error updating intern_profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lấy hoặc tạo mới university
     */
    private Integer getOrCreateUniversity(String universityName) {
        if (universityName == null || universityName.trim().isEmpty()) {
            return null;
        }

        String findSql = "SELECT uni_id FROM universities WHERE name_uni = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(findSql, universityName.trim());

        if (!result.isEmpty()) {
            return ((Number) result.get(0).get("uni_id")).intValue();
        }

        // Tạo mới
        String insertSql = "INSERT INTO universities (name_uni) VALUES (?)";
        jdbcTemplate.update(insertSql, universityName.trim());

        // Lấy ID vừa tạo
        result = jdbcTemplate.queryForList(findSql, universityName.trim());
        return ((Number) result.get(0).get("uni_id")).intValue();
    }

    /**
     * Lấy hoặc tạo mới major
     */
    private Integer getOrCreateMajor(String majorName) {
        if (majorName == null || majorName.trim().isEmpty()) {
            return null;
        }

        String findSql = "SELECT major_id FROM majors WHERE name_major = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(findSql, majorName.trim());

        if (!result.isEmpty()) {
            return ((Number) result.get(0).get("major_id")).intValue();
        }

        // Tạo mới
        String insertSql = "INSERT INTO majors (name_major) VALUES (?)";
        jdbcTemplate.update(insertSql, majorName.trim());

        // Lấy ID vừa tạo
        result = jdbcTemplate.queryForList(findSql, majorName.trim());
        return ((Number) result.get(0).get("major_id")).intValue();
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