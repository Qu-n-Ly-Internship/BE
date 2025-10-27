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
                SELECT m.mentor_id, m.fullname, u.email,
                       r.name as role_name,
                       d.name_department,
                       COUNT(DISTINCT ip.intern_id) as intern_count,
                       GROUP_CONCAT(DISTINCT ip.fullname SEPARATOR ', ') as intern_names
                FROM mentors m
                JOIN users u ON m.user_id = u.user_id
                LEFT JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN department d ON m.department_id = d.department_id
                LEFT JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                LEFT JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE u.status = 'ACTIVE'
                """);

            List<Object> params = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                sql.append(" AND (m.fullname LIKE ? OR u.email LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            if (department != null && !department.trim().isEmpty()) {
                sql.append(" AND d.name_department = ?");
                params.add(department);
            }

            sql.append(" GROUP BY m.mentor_id, m.fullname, u.email, r.name, d.name_department");
            sql.append(" ORDER BY m.fullname ASC");

            List<Map<String, Object>> mentors = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            var response = mentors.stream()
                    .map(mentorData -> Map.of(
                            "id", mentorData.get("mentor_id"),
                            "name", mentorData.get("fullname") != null ? mentorData.get("fullname") : "",
                            "email", mentorData.get("email") != null ? mentorData.get("email") : "",
                            "role", mentorData.get("role_name") != null ? mentorData.get("role_name") : "MENTOR",
                            "department", mentorData.get("name_department") != null ? mentorData.get("name_department") : "",
                            "internCount", mentorData.get("intern_count") != null ? mentorData.get("intern_count") : 0,
                            "internNames", mentorData.get("intern_names") != null ? mentorData.get("intern_names") : ""
                    ))
                    .toList();

            return Map.of(
                    "success", true,
                    "data", response,
                    "total", response.size()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy danh sách mentor: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMentorByIntern(Long internId) {
        try {
            String sql = """
            SELECT prog.mentor_id, m.fullname, u.email,
                   ip.available_from as start_date,
                   d.name_department as department_name,
                   d.department_id
            FROM intern_profiles ip
            JOIN intern_programs prog ON ip.program_id = prog.program_id
            JOIN mentors m ON prog.mentor_id = m.mentor_id
            JOIN users u ON m.user_id = u.user_id
            LEFT JOIN department d ON m.department_id = d.department_id
            WHERE ip.intern_id = ?
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

            String checkMentorSql = "SELECT COUNT(*) FROM mentors WHERE mentor_id = ?";
            int mentorCount = jdbcTemplate.queryForObject(checkMentorSql, Integer.class, mentorId);
            if (mentorCount == 0) {
                return Map.of("success", false, "message", "Mentor không tồn tại");
            }

            // Lấy program_id từ intern_profiles
            String getProgramSql = "SELECT program_id FROM intern_profiles WHERE intern_id = ?";
            Integer programId = jdbcTemplate.queryForObject(getProgramSql, Integer.class, internId);

            if (programId == null) {
                return Map.of("success", false, "message", "Thực tập sinh chưa có chương trình thực tập");
            }

            // Cập nhật mentor_id vào intern_programs
            String updateProgramSql = "UPDATE intern_programs SET mentor_id = ? WHERE program_id = ?";
            jdbcTemplate.update(updateProgramSql, mentorId, programId);

            return Map.of("success", true, "message", "Phân công mentor thành công!");

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

            // Lấy program_id từ intern_profiles
            String getProgramSql = "SELECT program_id FROM intern_profiles WHERE intern_id = ?";
            Integer programId = jdbcTemplate.queryForObject(getProgramSql, Integer.class, internId);

            if (programId == null) {
                return Map.of("success", false, "message", "Thực tập sinh chưa có chương trình");
            }

            if (mentorId == null) {
                // Xóa phân công: set mentor_id = NULL
                String updateSql = "UPDATE intern_programs SET mentor_id = NULL WHERE program_id = ?";
                jdbcTemplate.update(updateSql, programId);
                return Map.of("success", true, "message", "Đã xóa phân công mentor");
            }

            // Cập nhật mentor_id
            String updateSql = "UPDATE intern_programs SET mentor_id = ? WHERE program_id = ?";
            jdbcTemplate.update(updateSql, mentorId, programId);

            return Map.of("success", true, "message", "Lưu lựa chọn mentor thành công!");

        } catch (Exception e) {
            throw new RuntimeException("Lưu lựa chọn thất bại: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> checkMentorAssignment(Long internId) {
        try {
            String sql = """
                SELECT prog.mentor_id, m.fullname as mentor_name
                FROM intern_profiles ip
                JOIN intern_programs prog ON ip.program_id = prog.program_id
                JOIN mentors m ON prog.mentor_id = m.mentor_id
                WHERE ip.intern_id = ?
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
                WHERE p.mentor_id IS NULL OR p.program_id IS NULL
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
            SELECT 
                prog.mentor_id, 
                ip.intern_id, 
                ip.available_from as start_date,
                m.fullname as mentor_name, 
                u.email as mentor_email,
                ip.fullname as intern_name, 
                ip.phone as intern_phone,
                ip.email as intern_email,
                uni.name_uni as university_name,
                prog.program_id,
                prog.title as program_title
            FROM intern_programs prog
            JOIN intern_profiles ip ON prog.program_id = ip.program_id
            LEFT JOIN mentors m ON prog.mentor_id = m.mentor_id
            LEFT JOIN users u ON m.user_id = u.user_id
            LEFT JOIN universities uni ON ip.uni_id = uni.uni_id
            WHERE prog.mentor_id IS NOT NULL
            """);

            List<Object> params = new ArrayList<>();

            if (mentorId != null) {
                sql.append(" AND prog.mentor_id = ?");
                params.add(mentorId);
            }

            sql.append(" ORDER BY ip.available_from DESC");

            List<Map<String, Object>> assignments = jdbcTemplate.queryForList(
                    sql.toString(),
                    params.toArray()
            );

            return Map.of(
                    "success", true,
                    "data", assignments,
                    "total", assignments.size()
            );

        } catch (Exception e) {
            System.err.println("❌ Error in getAllAssignments: " + e.getMessage());
            e.printStackTrace();

            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách phân công: " + e.getMessage(),
                    "data", List.of(),
                    "total", 0
            );
        }
    }

    public Map<String, Object> unassignMentor(Long mentorId, Long internId) {
        try {
            // Lấy program_id từ intern_profiles
            String getProgramSql = "SELECT program_id FROM intern_profiles WHERE intern_id = ?";
            Integer programId = jdbcTemplate.queryForObject(getProgramSql, Integer.class, internId);

            if (programId == null) {
                return Map.of("success", false, "message", "Không tìm thấy thực tập sinh");
            }

            // Set mentor_id = NULL trong intern_programs
            String updateSql = "UPDATE intern_programs SET mentor_id = NULL WHERE program_id = ? AND mentor_id = ?";
            int affected = jdbcTemplate.update(updateSql, programId, mentorId);

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
            String mentorCountSql = "SELECT COUNT(*) FROM mentors m JOIN users u ON m.user_id = u.user_id WHERE u.status = 'ACTIVE'";
            int totalMentors = jdbcTemplate.queryForObject(mentorCountSql, Integer.class);

            String internCountSql = "SELECT COUNT(*) FROM intern_profiles";
            int totalInterns = jdbcTemplate.queryForObject(internCountSql, Integer.class);

            String assignedSql = """
                SELECT COUNT(DISTINCT ip.intern_id) 
                FROM intern_profiles ip
                JOIN intern_programs prog ON ip.program_id = prog.program_id
                WHERE prog.mentor_id IS NOT NULL
                """;
            int assignedInterns = jdbcTemplate.queryForObject(assignedSql, Integer.class);

            int unassignedInterns = totalInterns - assignedInterns;

            String topMentorSql = """
                SELECT m.fullname, COUNT(DISTINCT ip.intern_id) as intern_count
                FROM mentors m
                JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                GROUP BY m.mentor_id, m.fullname
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

    // ==================== API CHO MENTOR ====================

    public Map<String, Object> getInternsByMentor(Long mentorId) {
        try {
            String sql = """
                SELECT 
                    ip.intern_id,
                    ip.fullname as intern_name,
                    ip.email as intern_email,
                    ip.phone,
                    ip.year_of_study,
                    u.name_uni as university,
                    prog.title as program_title,
                    ip.available_from as assigned_date,
                    d.name_department as department
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                LEFT JOIN mentors m ON prog.mentor_id = m.mentor_id
                LEFT JOIN department d ON m.department_id = d.department_id
                WHERE prog.mentor_id = ?
                ORDER BY ip.available_from DESC
                """;

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql, mentorId);

            return Map.of(
                    "success", true,
                    "data", interns,
                    "total", interns.size()
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách thực tập sinh: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMentorPersonalStats(Long mentorId) {
        try {
            // Tổng số intern đang quản lý
            String totalSql = """
                SELECT COUNT(DISTINCT ip.intern_id) 
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE prog.mentor_id = ?
                """;
            int totalInterns = jdbcTemplate.queryForObject(totalSql, Integer.class, mentorId);

            // Phân bố theo trường
            String universitySql = """
                SELECT u.name_uni, COUNT(DISTINCT ip.intern_id) as count
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                JOIN universities u ON ip.uni_id = u.uni_id
                WHERE prog.mentor_id = ?
                GROUP BY u.name_uni
                ORDER BY count DESC
                """;
            List<Map<String, Object>> byUniversity = jdbcTemplate.queryForList(universitySql, mentorId);

            // Phân bố theo chương trình
            String programSql = """
                SELECT prog.title, COUNT(DISTINCT ip.intern_id) as count
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE prog.mentor_id = ?
                GROUP BY prog.title
                ORDER BY count DESC
                """;
            List<Map<String, Object>> byProgram = jdbcTemplate.queryForList(programSql, mentorId);

            // Intern mới nhất
            String recentSql = """
                SELECT ip.fullname, ip.available_from as start_date
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE prog.mentor_id = ?
                ORDER BY ip.available_from DESC
                LIMIT 5
                """;
            List<Map<String, Object>> recentInterns = jdbcTemplate.queryForList(recentSql, mentorId);

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalInterns", totalInterns,
                            "byUniversity", byUniversity,
                            "byProgram", byProgram,
                            "recentInterns", recentInterns
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage(), e);
        }
    }

    // ==================== API CHO ADMIN/HR ====================

    public Map<String, Object> getDetailedInternsByMentor(Long mentorId) {
        try {
            // Kiểm tra mentor tồn tại
            String checkMentorSql = "SELECT m.fullname, u.email FROM mentors m JOIN users u ON m.user_id = u.user_id WHERE m.mentor_id = ?";
            List<Map<String, Object>> mentorInfo = jdbcTemplate.queryForList(checkMentorSql, mentorId);

            if (mentorInfo.isEmpty()) {
                throw new RuntimeException("Không tìm thấy mentor với ID: " + mentorId);
            }

            String sql = """
                SELECT 
                    ip.intern_id,
                    ip.fullname as intern_name,
                    ip.email as intern_email,
                    ip.phone,
                    ip.dob,
                    ip.year_of_study,
                    ip.status as intern_status,
                    u.name_uni as university,
                    prog.title as program_title,
                    ip.available_from as assigned_date,
                    d.name_department as department,
                    
                    -- Số CV đã nộp
                    (SELECT COUNT(*) FROM cv WHERE intern_id = ip.intern_id) as cv_count,
                    
                    -- Số document đã nộp
                    (SELECT COUNT(*) FROM intern_documents WHERE intern_id = ip.intern_id) as document_count
                    
                FROM intern_programs prog
                JOIN intern_profiles ip ON prog.program_id = ip.program_id
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                LEFT JOIN mentors m ON prog.mentor_id = m.mentor_id
                LEFT JOIN department d ON m.department_id = d.department_id
                WHERE prog.mentor_id = ?
                ORDER BY ip.available_from DESC
                """;

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql, mentorId);

            return Map.of(
                    "success", true,
                    "mentor", Map.of(
                            "id", mentorId,
                            "name", mentorInfo.get(0).get("fullname"),
                            "email", mentorInfo.get(0).get("email")
                    ),
                    "interns", interns,
                    "total", interns.size()
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách thực tập sinh: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMentorOverview() {
        try {
            String sql = """
                SELECT 
                    m.mentor_id,
                    m.fullname as mentor_name,
                    u.email as mentor_email,
                    r.name as role_name,
                    COUNT(DISTINCT ip.intern_id) as intern_count,
                    
                    -- Số intern theo trạng thái
                    COUNT(DISTINCT CASE WHEN ip.status = 'ACTIVE' THEN ip.intern_id END) as active_interns,
                    COUNT(DISTINCT CASE WHEN ip.status = 'PENDING' THEN ip.intern_id END) as pending_interns,
                    COUNT(DISTINCT CASE WHEN ip.status = 'COMPLETED' THEN ip.intern_id END) as completed_interns,
                    
                    -- Ngày phân công gần nhất
                    MAX(ip.available_from) as latest_assignment,
                    
                    -- Department
                    d.name_department as department
                    
                FROM mentors m
                JOIN users u ON m.user_id = u.user_id
                JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN department d ON m.department_id = d.department_id
                LEFT JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                LEFT JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE u.status = 'ACTIVE'
                GROUP BY m.mentor_id, m.fullname, u.email, r.name, d.name_department
                ORDER BY intern_count DESC, m.fullname ASC
                """;

            List<Map<String, Object>> mentors = jdbcTemplate.queryForList(sql);

            // Tính tổng số
            int totalMentors = mentors.size();
            int totalInterns = mentors.stream()
                    .mapToInt(mentorData -> ((Number) mentorData.get("intern_count")).intValue())
                    .sum();

            double avgInternsPerMentor = totalMentors > 0 ? (double) totalInterns / totalMentors : 0;

            return Map.of(
                    "success", true,
                    "summary", Map.of(
                            "totalMentors", totalMentors,
                            "totalInterns", totalInterns,
                            "avgInternsPerMentor", String.format("%.1f", avgInternsPerMentor)
                    ),
                    "mentors", mentors
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy tổng quan mentor: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getWorkloadDistribution() {
        try {
            // Mentor có nhiều intern nhất
            String topSql = """
                SELECT 
                    m.mentor_id as user_id,
                    m.fullname,
                    u.email,
                    COUNT(DISTINCT ip.intern_id) as intern_count
                FROM mentors m
                JOIN users u ON m.user_id = u.user_id
                LEFT JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                LEFT JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE u.status = 'ACTIVE'
                GROUP BY m.mentor_id, m.fullname, u.email
                HAVING intern_count > 0
                ORDER BY intern_count DESC
                LIMIT 10
                """;
            List<Map<String, Object>> topMentors = jdbcTemplate.queryForList(topSql);

            // Mentor chưa có intern hoặc có ít intern nhất
            String availableSql = """
                SELECT 
                    m.mentor_id as user_id,
                    m.fullname,
                    u.email,
                    COUNT(DISTINCT ip.intern_id) as intern_count
                FROM mentors m
                JOIN users u ON m.user_id = u.user_id
                LEFT JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                LEFT JOIN intern_profiles ip ON prog.program_id = ip.program_id
                WHERE u.status = 'ACTIVE'
                GROUP BY m.mentor_id, m.fullname, u.email
                ORDER BY intern_count ASC
                LIMIT 10
                """;
            List<Map<String, Object>> availableMentors = jdbcTemplate.queryForList(availableSql);

            // Phân bố theo khoảng
            String distributionSql = """
                SELECT 
                    CASE 
                        WHEN intern_count = 0 THEN '0 intern'
                        WHEN intern_count BETWEEN 1 AND 3 THEN '1-3 interns'
                        WHEN intern_count BETWEEN 4 AND 6 THEN '4-6 interns'
                        WHEN intern_count BETWEEN 7 AND 10 THEN '7-10 interns'
                        ELSE '10+ interns'
                    END as workload_range,
                    COUNT(*) as mentor_count
                FROM (
                    SELECT m.mentor_id, COUNT(DISTINCT ip.intern_id) as intern_count
                    FROM mentors m
                    JOIN users u ON m.user_id = u.user_id
                    LEFT JOIN intern_programs prog ON m.mentor_id = prog.mentor_id
                    LEFT JOIN intern_profiles ip ON prog.program_id = ip.program_id
                    WHERE u.status = 'ACTIVE'
                    GROUP BY m.mentor_id
                ) workload
                GROUP BY workload_range
                ORDER BY 
                    CASE workload_range
                        WHEN '0 intern' THEN 1
                        WHEN '1-3 interns' THEN 2
                        WHEN '4-6 interns' THEN 3
                        WHEN '7-10 interns' THEN 4
                        ELSE 5
                    END
                """;
            List<Map<String, Object>> distribution = jdbcTemplate.queryForList(distributionSql);

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "topMentors", topMentors,
                            "availableMentors", availableMentors,
                            "distribution", distribution
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy phân bố workload: " + e.getMessage(), e);
        }
    }
}