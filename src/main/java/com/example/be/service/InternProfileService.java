package com.example.be.service;

import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternProfileService {
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    // Giới hạn số người trong mỗi nhóm
    private static final int MAX_GROUP_SIZE = 8;

    public Map<String, Object> getAllProfiles(String query, String school, String major, String status, int page, int size) {
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT ip.intern_id as intern_id, ip.fullname as student, ip.email as studentEmail,
                           u.name_uni as school, ip.major_id, ip.year_of_study,
                           ip.phone, ip.available_from as startDate, ip.end_date as endDate,
                           p.title, ip.status, ip.program_id,
                           mentor.fullname as mentor_name, p.mentor_id
                    FROM intern_profiles ip
                    LEFT JOIN universities u ON ip.uni_id = u.uni_id
                    LEFT JOIN intern_programs p ON ip.program_id = p.program_id
                    LEFT JOIN users mentor ON p.mentor_id = mentor.user_id
                    WHERE 1=1
                    """);

            List<Object> params = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                sql.append(" AND (ip.fullname LIKE ? OR ip.email LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            if (school != null && !school.trim().isEmpty()) {
                sql.append(" AND u.name_uni = ?");
                params.add(school);
            }

            if (major != null && !major.trim().isEmpty()) {
                sql.append(" AND ip.major_id = ?");
                params.add(Integer.parseInt(major));
            }

            if (status != null && !status.trim().isEmpty()) {
                sql.append(" AND ip.status = ?");
                params.add(status);
            }

            sql.append(" ORDER BY ip.intern_id DESC LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> profiles = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            profiles.forEach(p -> {
                Object majorId = p.get("major_id");
                p.put("major", getMajorName(majorId));
            });

            int total = getTotalProfileCount(query, school, major, status);

            return Map.of(
                    "success", true,
                    "data", profiles,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", total,
                            "totalPages", (int) Math.ceil((double) total / size)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải danh sách: " + e.getMessage());
        }
    }

    public Map<String, Object> createProfile(Map<String, Object> request) {
        try {
            validateProfileRequest(request);

            Integer uniId = getOrCreateUniversity((String) request.get("school"));
            Integer majorId = getMajorId((String) request.get("major"));

            // ✅ SỬA: Tự động phân nhóm với giới hạn 8 người
            Integer programId = getOrCreateProgramWithLimit(request);

            String insertSql = """
                    INSERT INTO intern_profiles 
                    (fullname, email, uni_id, major_id, program_id, available_from, end_date, status, phone, year_of_study)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, '', 0)
                    """;

            jdbcTemplate.update(insertSql,
                    ((String) request.get("student")).trim(),
                    ((String) request.get("studentEmail")).trim(),
                    uniId,
                    majorId,
                    programId,
                    request.get("startDate"),
                    request.get("endDate"),
                    request.getOrDefault("status", "active")
            );

            return Map.of(
                    "success", true,
                    "message", "Thêm thực tập sinh thành công!"
            );
        } catch (Exception e) {
            throw new RuntimeException("Thêm thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> updateProfile(Long id, Map<String, Object> request) {
        try {
            if (!profileExists(id)) {
                throw new RuntimeException("Không tìm thấy thực tập sinh với ID: " + id);
            }

            Integer uniId = getOrCreateUniversity((String) request.get("school"));
            Integer majorId = getMajorId((String) request.get("major"));
            Integer programId = updateOrCreateProgram(id, request);

            String updateSql = """
                    UPDATE intern_profiles 
                    SET fullname = ?, 
                        email = ?, 
                        uni_id = ?, 
                        major_id = ?,
                        program_id = ?,
                        available_from = ?, 
                        end_date = ?, 
                        status = ?
                    WHERE intern_id = ?
                    """;

            jdbcTemplate.update(updateSql,
                    request.get("student"),
                    request.get("studentEmail"),
                    uniId,
                    majorId,
                    programId,
                    request.get("startDate"),
                    request.get("endDate"),
                    request.get("status"),
                    id
            );

            return Map.of(
                    "success", true,
                    "message", "Cập nhật thành công!"
            );
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật thất bại: " + e.getMessage());
        }
    }

    public void deleteProfile(Long id) {
        try {
            if (!profileExists(id)) {
                throw new RuntimeException("Không tìm thấy thực tập sinh với ID: " + id);
            }

            String sql = "DELETE FROM intern_profiles WHERE intern_id = ?";
            jdbcTemplate.update(sql, id);
        } catch (Exception e) {
            throw new RuntimeException("Xóa thất bại: " + e.getMessage());
        }
    }

    private boolean profileExists(Long id) {
        String sql = "SELECT COUNT(*) FROM intern_profiles WHERE intern_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, id) > 0;
    }

    private void validateProfileRequest(Map<String, Object> request) {
        String student = (String) request.get("student");
        String studentEmail = (String) request.get("studentEmail");

        if (student == null || student.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên sinh viên");
        }

        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        String checkEmailSql = "SELECT COUNT(*) FROM intern_profiles WHERE email = ?";
        int count = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, studentEmail);
        if (count > 0) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống");
        }
    }

    private Integer getOrCreateUniversity(String schoolName) {
        if (schoolName == null || schoolName.trim().isEmpty()) {
            return null;
        }

        String findUniSql = "SELECT uni_id FROM universities WHERE name_uni = ?";
        List<Map<String, Object>> uniResult = jdbcTemplate.queryForList(findUniSql, schoolName);

        if (!uniResult.isEmpty()) {
            return (Integer) uniResult.get(0).get("uni_id");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO universities (name_uni) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, schoolName);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    private Integer getMajorId(String majorName) {
        if (majorName == null || majorName.trim().isEmpty()) {
            return null;
        }

        String findSql = "SELECT major_id FROM majors WHERE name_major = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(findSql, majorName.trim());

        if (!result.isEmpty()) {
            return (Integer) result.get(0).get("major_id");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO majors (name_major) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, majorName.trim());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    /**
     * ✅ PHƯƠNG THỨC MỚI: Tự động phân nhóm với giới hạn 8 người
     * - Tìm các program có cùng title
     * - Kiểm tra số lượng thành viên trong mỗi program
     * - Nếu program chưa đủ 8 người -> dùng program đó
     * - Nếu tất cả program đều đủ 8 người -> tạo program mới
     */
    private Integer getOrCreateProgramWithLimit(Map<String, Object> request) {
        String title = (String) request.get("title");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        // Tìm tất cả các program có cùng title
        String findProgramsSql = """
                SELECT p.program_id, COUNT(ip.intern_id) as member_count
                FROM intern_programs p
                LEFT JOIN intern_profiles ip ON p.program_id = ip.program_id
                WHERE p.title = ?
                GROUP BY p.program_id
                HAVING COUNT(ip.intern_id) < ?
                ORDER BY p.program_id ASC
                LIMIT 1
                """;

        List<Map<String, Object>> availablePrograms = jdbcTemplate.queryForList(
                findProgramsSql,
                title.trim(),
                MAX_GROUP_SIZE
        );

        // Nếu có program chưa đầy -> dùng program đó
        if (!availablePrograms.isEmpty()) {
            return (Integer) availablePrograms.get(0).get("program_id");
        }

        // Nếu tất cả program đều đầy -> tạo program mới
        KeyHolder programKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO intern_programs (title, description, capacity, start_date, end_date) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, title.trim());
            ps.setString(2, "Nhóm thực tập " + title.trim());
            ps.setInt(3, MAX_GROUP_SIZE);
            ps.setString(4, (String) request.get("startDate"));
            ps.setString(5, (String) request.get("endDate"));
            return ps;
        }, programKeyHolder);

        return programKeyHolder.getKey().intValue();
    }

    private Integer updateOrCreateProgram(Long internId, Map<String, Object> request) {
        String title = (String) request.get("title");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String getProgramSql = "SELECT program_id FROM intern_profiles WHERE intern_id = ?";
        Integer currentProgramId = jdbcTemplate.queryForObject(getProgramSql, Integer.class, internId);

        if (currentProgramId != null) {
            jdbcTemplate.update(
                    "UPDATE intern_programs SET title = ?, start_date = ?, end_date = ? WHERE program_id = ?",
                    title.trim(),
                    request.get("startDate"),
                    request.get("endDate"),
                    currentProgramId
            );
            return currentProgramId;
        }

        // Dùng logic tự động phân nhóm
        return getOrCreateProgramWithLimit(request);
    }

    private int getTotalProfileCount(String query, String school, String major, String status) {
        StringBuilder countSql = new StringBuilder("""
                SELECT COUNT(*) FROM intern_profiles ip
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            countSql.append(" AND (ip.fullname LIKE ? OR ip.email LIKE ?)");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (school != null && !school.trim().isEmpty()) {
            countSql.append(" AND u.name_uni = ?");
            params.add(school);
        }

        if (major != null && !major.trim().isEmpty()) {
            countSql.append(" AND ip.major_id = ?");
            params.add(Integer.parseInt(major));
        }

        if (status != null && !status.trim().isEmpty()) {
            countSql.append(" AND ip.status = ?");
            params.add(status);
        }

        return jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());
    }

    private String getMajorName(Object majorId) {
        if (majorId == null) return "";

        try {
            int id = (Integer) majorId;
            String sql = "SELECT name_major FROM majors WHERE major_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (!result.isEmpty()) {
                return (String) result.get(0).get("name_major");
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}