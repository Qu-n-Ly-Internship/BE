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
            SELECT 
                prog.mentor_id, 
                ip.intern_id, 
                ip.available_from as start_date,
                u.fullname as mentor_name, 
                u.email as mentor_email,
                ip.fullname as intern_name, 
                ip.phone as intern_phone,
                ip.email as intern_email,
                uni.name_uni as university_name,
                prog.program_id,
                prog.title as program_title
            FROM intern_programs prog
            JOIN intern_profiles ip ON prog.program_id = ip.program_id
            LEFT JOIN users u ON prog.mentor_id = u.user_id
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
            // Log chi tiết để debug
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

    // ==================== API CHO MENTOR ====================

    // 1️⃣ Lấy danh sách intern của mentor (cho chính mentor đó)
    public Map<String, Object> getInternsByMentor(Long mentorId) {
        try {
            String sql = """
                SELECT 
                    i.intern_id,
                    i.fullname as intern_name,
                    i.email as intern_email,
                    i.phone,
                    i.year_of_study,
                    u.name_uni as university,
                    p.title as program_title,
                    ma.start_date as assigned_date,
                    d.name_department as department
                FROM mentor_assignments ma
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                LEFT JOIN department d ON ma.department_id = d.department_id
                WHERE ma.mentor_id = ?
                ORDER BY ma.start_date DESC
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

    // 2️⃣ Thống kê cá nhân của mentor
    public Map<String, Object> getMentorPersonalStats(Long mentorId) {
        try {
            // Tổng số intern đang quản lý
            String totalSql = """
                SELECT COUNT(*) 
                FROM mentor_assignments 
                WHERE mentor_id = ?
                """;
            int totalInterns = jdbcTemplate.queryForObject(totalSql, Integer.class, mentorId);

            // Phân bố theo trường
            String universitySql = """
                SELECT u.name_uni, COUNT(*) as count
                FROM mentor_assignments ma
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                JOIN universities u ON i.uni_id = u.uni_id
                WHERE ma.mentor_id = ?
                GROUP BY u.name_uni
                ORDER BY count DESC
                """;
            List<Map<String, Object>> byUniversity = jdbcTemplate.queryForList(universitySql, mentorId);

            // Phân bố theo chương trình
            String programSql = """
                SELECT p.title, COUNT(*) as count
                FROM mentor_assignments ma
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                JOIN intern_programs p ON i.program_id = p.program_id
                WHERE ma.mentor_id = ?
                GROUP BY p.title
                ORDER BY count DESC
                """;
            List<Map<String, Object>> byProgram = jdbcTemplate.queryForList(programSql, mentorId);

            // Intern mới nhất
            String recentSql = """
                SELECT i.fullname, ma.start_date
                FROM mentor_assignments ma
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                WHERE ma.mentor_id = ?
                ORDER BY ma.start_date DESC
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

    // ==================== API MỚI: CHO ADMIN/HR ====================

    // 3️⃣ Lấy chi tiết thực tập sinh của mentor (version đầy đủ cho Admin/HR)
    public Map<String, Object> getDetailedInternsByMentor(Long mentorId) {
        try {
            // Kiểm tra mentor tồn tại
            String checkMentorSql = "SELECT fullname, email FROM users WHERE user_id = ?";
            List<Map<String, Object>> mentorInfo = jdbcTemplate.queryForList(checkMentorSql, mentorId);

            if (mentorInfo.isEmpty()) {
                throw new RuntimeException("Không tìm thấy mentor với ID: " + mentorId);
            }

            String sql = """
                SELECT 
                    i.intern_id,
                    i.fullname as intern_name,
                    i.email as intern_email,
                    i.phone,
                    i.dob,
                    i.year_of_study,
                    i.status as intern_status,
                    u.name_uni as university,
                    m.name_major as major,
                    p.title as program_title,
                    p.start_date as program_start,
                    p.end_date as program_end,
                    ma.start_date as assigned_date,
                    d.name_department as department,
                    
                    -- Số CV đã nộp
                    (SELECT COUNT(*) FROM cv WHERE intern_id = i.intern_id) as cv_count,
                    
                    -- Số document đã nộp
                    (SELECT COUNT(*) FROM intern_documents WHERE intern_id = i.intern_id) as document_count
                    
                FROM mentor_assignments ma
                JOIN intern_profiles i ON ma.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN majors m ON i.major_id = m.major_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                LEFT JOIN department d ON ma.department_id = d.department_id
                WHERE ma.mentor_id = ?
                ORDER BY ma.start_date DESC
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

    // 4️⃣ Tổng quan tất cả mentor (cho Admin/HR Dashboard)
    public Map<String, Object> getMentorOverview() {
        try {
            String sql = """
                SELECT 
                    u.user_id as mentor_id,
                    u.fullname as mentor_name,
                    u.email as mentor_email,
                    r.name as role_name,
                    COUNT(DISTINCT ma.intern_id) as intern_count,
                    
                    -- Số intern theo trạng thái
                    COUNT(DISTINCT CASE WHEN i.status = 'ACTIVE' THEN ma.intern_id END) as active_interns,
                    COUNT(DISTINCT CASE WHEN i.status = 'PENDING' THEN ma.intern_id END) as pending_interns,
                    COUNT(DISTINCT CASE WHEN i.status = 'COMPLETED' THEN ma.intern_id END) as completed_interns,
                    
                    -- Ngày phân công gần nhất
                    MAX(ma.start_date) as latest_assignment,
                    
                    -- Department
                    GROUP_CONCAT(DISTINCT d.name_department SEPARATOR ', ') as departments
                    
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN mentor_assignments ma ON u.user_id = ma.mentor_id
                LEFT JOIN intern_profiles i ON ma.intern_id = i.intern_id
                LEFT JOIN department d ON ma.department_id = d.department_id
                WHERE (r.name = 'MENTOR' OR r.name = 'HR' OR r.name = 'ADMIN')
                  AND u.status = 'ACTIVE'
                GROUP BY u.user_id, u.fullname, u.email, r.name
                ORDER BY intern_count DESC, u.fullname ASC
                """;

            List<Map<String, Object>> mentors = jdbcTemplate.queryForList(sql);

            // Tính tổng số
            int totalMentors = mentors.size();
            int totalInterns = mentors.stream()
                    .mapToInt(m -> ((Number) m.get("intern_count")).intValue())
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

    // 5️⃣ Phân bố khối lượng công việc (workload distribution)
    public Map<String, Object> getWorkloadDistribution() {
        try {
            // Mentor có nhiều intern nhất
            String topSql = """
                SELECT 
                    u.user_id,
                    u.fullname,
                    u.email,
                    COUNT(ma.intern_id) as intern_count
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN mentor_assignments ma ON u.user_id = ma.mentor_id
                WHERE (r.name = 'MENTOR' OR r.name = 'HR' OR r.name = 'ADMIN')
                  AND u.status = 'ACTIVE'
                GROUP BY u.user_id, u.fullname, u.email
                HAVING intern_count > 0
                ORDER BY intern_count DESC
                LIMIT 10
                """;
            List<Map<String, Object>> topMentors = jdbcTemplate.queryForList(topSql);

            // Mentor chưa có intern hoặc có ít intern nhất
            String availableSql = """
                SELECT 
                    u.user_id,
                    u.fullname,
                    u.email,
                    COUNT(ma.intern_id) as intern_count
                FROM users u
                JOIN roles r ON u.role_id = r.role_id
                LEFT JOIN mentor_assignments ma ON u.user_id = ma.mentor_id
                WHERE (r.name = 'MENTOR' OR r.name = 'HR' OR r.name = 'ADMIN')
                  AND u.status = 'ACTIVE'
                GROUP BY u.user_id, u.fullname, u.email
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
                    SELECT u.user_id, COUNT(ma.intern_id) as intern_count
                    FROM users u
                    JOIN roles r ON u.role_id = r.role_id
                    LEFT JOIN mentor_assignments ma ON u.user_id = ma.mentor_id
                    WHERE (r.name = 'MENTOR' OR r.name = 'HR' OR r.name = 'ADMIN')
                      AND u.status = 'ACTIVE'
                    GROUP BY u.user_id
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