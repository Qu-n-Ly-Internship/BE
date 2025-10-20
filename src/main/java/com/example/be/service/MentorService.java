package com.example.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MentorService {
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getAllMentors(String query, String department, Boolean available) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT u.user_id, u.fullname, u.email,
                       r.name as role_name,
                       COUNT(DISTINCT ma.intern_id) as intern_count,
                       GROUP_CONCAT(DISTINCT i.fullname SEPARATOR ', ') as intern_names
                FROM users u
                LEFT JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN mentor_assignments ma ON u.user_id = ma.mentor_id
                LEFT JOIN intern_profiles i ON ma.intern_id = i.intern_id
                WHERE (r.name = 'MENTOR' OR r.name = 'HR' OR r.name = 'ADMIN')
                  AND u.status = 'ACTIVE'
                """);

            List<Object> params = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                sql.append(" AND (u.fullname LIKE ? OR u.email LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            sql.append(" GROUP BY u.user_id, u.fullname, u.email, r.name ORDER BY u.fullname ASC");

            List<Map<String, Object>> mentors = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            var response = mentors.stream()
                    .map(m -> Map.of(
                            "id", m.get("user_id"),
                            "name", m.get("fullname") != null ? m.get("fullname") : "",
                            "email", m.get("email") != null ? m.get("email") : "",
                            "role", m.get("role_name") != null ? m.get("role_name") : "MENTOR",
                            "internCount", m.get("intern_count") != null ? m.get("intern_count") : 0,
                            "internNames", m.get("intern_names") != null ? m.get("intern_names") : ""
                    ))
                    .toList();

            return Map.of(
                    "success", true,
                    "data", response,
                    "total", response.size()
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách mentor: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMentorByIntern(Long internId) {
        try {
            String sql = """
            SELECT ma.mentor_id, u.fullname, u.email, 
                   ma.start_date, ma.department_id,
                   d.name_department as department_name
            FROM mentor_assignments ma
            JOIN users u ON ma.mentor_id = u.user_id
            LEFT JOIN department d ON ma.department_id = d.department_id
            WHERE ma.intern_id = ?
            ORDER BY ma.start_date DESC
            LIMIT 1
            """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, internId);

            if (result.isEmpty()) {
                return Map.of("success", true, "data", null, "message", "Chưa có mentor được phân công");
            }

            Map<String, Object> mentor = result.get(0);
            Map<String, Object> mentorData = new HashMap<>();
            mentorData.put("mentorId", mentor.get("mentor_id"));
            mentorData.put("mentorName", mentor.get("fullname") != null ? mentor.get("fullname") : "");
            mentorData.put("mentorEmail", mentor.get("email") != null ? mentor.get("email") : "");
            mentorData.put("startDate", mentor.get("start_date") != null ? mentor.get("start_date").toString() : "");
            mentorData.put("departmentId", mentor.get("department_id"));
            mentorData.put("departmentName", mentor.get("department_name") != null ? mentor.get("department_name") : "");

            return Map.of("success", true, "data", mentorData);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thông tin mentor: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> assignMentor(Map<String, Object> request) {
        try {
            Long mentorId = request.get("mentorId") != null
                    ? ((Number) request.get("mentorId")).longValue()
                    : null;
            Long internId = request.get("internId") != null
                    ? ((Number) request.get("internId")).longValue()
                    : null;
            Integer departmentId = request.get("departmentId") != null
                    ? (Integer) request.get("departmentId")
                    : null;
            String startDate = (String) request.get("startDate");

            if (mentorId == null || internId == null) {
                return Map.of("success", false, "message", "Vui lòng chọn mentor và thực tập sinh");
            }

            String checkMentorSql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
            int mentorCount = jdbcTemplate.queryForObject(checkMentorSql, Integer.class, mentorId);
            if (mentorCount == 0) {
                return Map.of("success", false, "message", "Mentor không tồn tại");
            }

            String checkInternSql = "SELECT COUNT(*) FROM intern_profiles WHERE intern_id = ?";
            int internCount = jdbcTemplate.queryForObject(checkInternSql, Integer.class, internId);
            if (internCount == 0) {
                return Map.of("success", false, "message", "Thực tập sinh không tồn tại");
            }

            String checkAssignmentSql = """
                SELECT COUNT(*) FROM mentor_assignments 
                WHERE mentor_id = ? AND intern_id = ?
                """;
            int assignmentCount = jdbcTemplate.queryForObject(checkAssignmentSql, Integer.class, mentorId, internId);

            if (assignmentCount > 0) {
                String updateSql = """
                    UPDATE mentor_assignments 
                    SET department_id = ?, start_date = COALESCE(?, start_date, NOW())
                    WHERE mentor_id = ? AND intern_id = ?
                    """;
                jdbcTemplate.update(updateSql, departmentId, startDate, mentorId, internId);

                return Map.of("success", true, "message", "Cập nhật phân công mentor thành công!");
            } else {
                String insertSql = """
                    INSERT INTO mentor_assignments (mentor_id, intern_id, department_id, start_date)
                    VALUES (?, ?, ?, COALESCE(?, NOW()))
                    """;
                jdbcTemplate.update(insertSql, mentorId, internId, departmentId, startDate);

                return Map.of("success", true, "message", "Phân công mentor thành công!");
            }

        } catch (Exception e) {
            throw new RuntimeException("Phân công thất bại: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> saveMentorSelection(Map<String, Object> request) {
        try {
            Long internId = request.get("internId") != null
                    ? ((Number) request.get("internId")).longValue()
                    : null;
            Long mentorId = request.get("mentorId") != null
                    ? ((Number) request.get("mentorId")).longValue()
                    : null;

            if (internId == null) {
                return Map.of("success", false, "message", "Thiếu thông tin thực tập sinh");
            }

            if (mentorId == null) {
                String deleteSql = "DELETE FROM mentor_assignments WHERE intern_id = ?";
                jdbcTemplate.update(deleteSql, internId);
                return Map.of("success", true, "message", "Đã xóa phân công mentor");
            }

            String checkSql = "SELECT COUNT(*) FROM mentor_assignments WHERE intern_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, internId);

            if (count > 0) {
                String updateSql = """
                    UPDATE mentor_assignments 
                    SET mentor_id = ?, start_date = NOW()
                    WHERE intern_id = ?
                    """;
                jdbcTemplate.update(updateSql, mentorId, internId);
            } else {
                String insertSql = """
                    INSERT INTO mentor_assignments (mentor_id, intern_id, start_date)
                    VALUES (?, ?, NOW())
                    """;
                jdbcTemplate.update(insertSql, mentorId, internId);
            }

            return Map.of("success", true, "message", "Lưu lựa chọn mentor thành công!");

        } catch (Exception e) {
            throw new RuntimeException("Lưu lựa chọn thất bại: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> checkMentorAssignment(Long internId) {
        try {
            String sql = """
                SELECT ma.mentor_id, u.fullname as mentor_name
                FROM mentor_assignments ma
                JOIN users u ON ma.mentor_id = u.user_id
                WHERE ma.intern_id = ?
                LIMIT 1
                """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, internId);

            return Map.of(
                    "success", true,
                    "hasAssignment", !result.isEmpty(),
                    "data", result.isEmpty() ? null : result.get(0)
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi kiểm tra: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getUnassignedInterns() {
        try {
            String sql = """
                SELECT i.intern_id, i.fullname, i.phone, i.email,
                       u.name_uni as university_name,
                       p.title as program_title
                FROM intern_profiles i
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                LEFT JOIN mentor_assignments ma ON i.intern_id = ma.intern_id
                WHERE ma.mentor_id IS NULL
                ORDER BY i.fullname ASC
                """;

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql);

            return Map.of("success", true, "data", interns, "total", interns.size());

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getAllAssignments(Long mentorId) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT ma.mentor_id, ma.intern_id, ma.start_date,
                       u.fullname as mentor_name, u.email as mentor_email,
                       i.fullname as intern_name, i.phone as intern_phone,
                       uni.name_uni as university_name,
                       d.name_department as department_name
                FROM mentor_assignments ma
                JOIN users u ON ma.mentor_id = u.user_id
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                LEFT JOIN universities uni ON i.uni_id = uni.uni_id
                LEFT JOIN department d ON ma.department_id = d.department_id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            if (mentorId != null) {
                sql.append(" AND ma.mentor_id = ?");
                params.add(mentorId);
            }

            sql.append(" ORDER BY ma.start_date DESC");

            List<Map<String, Object>> assignments = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            return Map.of("success", true, "data", assignments, "total", assignments.size());

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phân công: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> unassignMentor(Long mentorId, Long internId) {
        try {
            String sql = "DELETE FROM mentor_assignments WHERE mentor_id = ? AND intern_id = ?";
            int affected = jdbcTemplate.update(sql, mentorId, internId);

            if (affected == 0) {
                return Map.of("success", false, "message", "Không tìm thấy phân công này");
            }

            return Map.of("success", true, "message", "Hủy phân công mentor thành công!");

        } catch (Exception e) {
            throw new RuntimeException("Hủy phân công thất bại: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMentorStats() {
        try {
            String mentorCountSql = """
                SELECT COUNT(DISTINCT u.user_id) 
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE r.name IN ('MENTOR', 'HR', 'ADMIN')
                  AND u.status = 'ACTIVE'
                """;
            int totalMentors = jdbcTemplate.queryForObject(mentorCountSql, Integer.class);

            String internCountSql = "SELECT COUNT(*) FROM intern_profiles";
            int totalInterns = jdbcTemplate.queryForObject(internCountSql, Integer.class);

            String assignedSql = "SELECT COUNT(DISTINCT intern_id) FROM mentor_assignments";
            int assignedInterns = jdbcTemplate.queryForObject(assignedSql, Integer.class);

            int unassignedInterns = totalInterns - assignedInterns;

            String topMentorSql = """
                SELECT u.fullname, COUNT(ma.intern_id) as intern_count
                FROM mentor_assignments ma
                JOIN users u ON ma.mentor_id = u.user_id
                GROUP BY u.user_id, u.fullname
                ORDER BY intern_count DESC
                LIMIT 5
                """;
            List<Map<String, Object>> topMentors = jdbcTemplate.queryForList(topMentorSql);

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalMentors", totalMentors,
                            "totalInterns", totalInterns,
                            "assignedInterns", assignedInterns,
                            "unassignedInterns", unassignedInterns,
                            "assignmentRate", totalInterns > 0
                                    ? String.format("%.1f%%", (assignedInterns * 100.0 / totalInterns))
                                    : "0%",
                            "topMentors", topMentors
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage(), e);
        }
    }
}

