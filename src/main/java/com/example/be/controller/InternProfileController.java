package com.example.be.controller;

import com.example.be.service.InternProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/intern-profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class InternProfileController {

    private final InternProfileService internProfileService;

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // 1. Lấy danh sách intern profiles với filter
    @GetMapping("")
    public ResponseEntity<?> getAllProfiles(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "school", required = false) String school,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "mentorUserId", required = false) Long mentorUserId, // ✅ THÊM PARAMETER NÀY
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size) {
        try {
            Map<String, Object> result;

            // ✅ Nếu có mentorUserId -> Chỉ lấy intern thuộc program của mentor
            if (mentorUserId != null) {
                result = getInternsByMentor(mentorUserId, query, status, page, size);
            }
            // ✅ Nếu không có mentorUserId -> Lấy tất cả (Admin/HR)
            else {
                result = internProfileService.getAllProfiles(query, school, major, status, page, size);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải danh sách: " + e.getMessage()));
        }
    }
    // ✅ NEW METHOD: Lấy intern theo mentor
    private Map<String, Object> getInternsByMentor(Long mentorUserId, String query, String status, int page, int size) {
        try {
            // Bước 1: Tìm mentor_id từ user_id
            String findMentorSql = "SELECT mentor_id FROM mentors WHERE user_id = ?";
            List<Map<String, Object>> mentorResult = jdbcTemplate.queryForList(findMentorSql, mentorUserId);

            if (mentorResult.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "Không tìm thấy thông tin mentor"
                );
            }

            Long mentorId = ((Number) mentorResult.get(0).get("mentor_id")).longValue();

            // Bước 2: Query intern thuộc program của mentor
            String sql = """
                    SELECT DISTINCT
                        ip.intern_id,
                        ip.fullname,
                        ip.email,
                        ip.phone,
                        ip.status,
                        ip.available_from,
                        ip.end_date,
                        ip.dob,
                        ip.year_of_study,
                        prog.title as program_name,
                        prog.program_id,
                        u.name_uni as university_name,
                        m.name_major as major_name
                    FROM intern_profiles ip
                    INNER JOIN intern_programs prog ON ip.program_id = prog.program_id
                    LEFT JOIN universities u ON ip.uni_id = u.uni_id
                    LEFT JOIN majors m ON ip.major_id = m.major_id
                    WHERE prog.mentor_id = ?
                    """ +
                    (query != null && !query.trim().isEmpty() ?
                            " AND (ip.fullname LIKE ? OR ip.email LIKE ?)" : "") +
                    (status != null && !status.trim().isEmpty() ?
                            " AND ip.status = ?" : "") +
                    " ORDER BY ip.fullname ASC";

            // Build parameters
            List<Object> params = new ArrayList<>();
            params.add(mentorId);

            if (query != null && !query.trim().isEmpty()) {
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            if (status != null && !status.trim().isEmpty()) {
                params.add(status);
            }

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql, params.toArray());

            // Format response
            List<Map<String, Object>> formattedInterns = new ArrayList<>();
            for (Map<String, Object> intern : interns) {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("intern_id", intern.get("intern_id"));
                formatted.put("id", intern.get("intern_id")); // Alias cho frontend
                formatted.put("student", intern.get("fullname")); // ✅ Tên field frontend đang dùng
                formatted.put("fullname", intern.get("fullname"));
                formatted.put("studentEmail", intern.get("email")); // ✅ Tên field frontend đang dùng
                formatted.put("email", intern.get("email"));
                formatted.put("phone", intern.get("phone"));
                formatted.put("status", intern.get("status"));
                formatted.put("availableFrom", intern.get("available_from"));
                formatted.put("endDate", intern.get("end_date"));
                formatted.put("dob", intern.get("dob"));
                formatted.put("yearOfStudy", intern.get("year_of_study"));
                formatted.put("programName", intern.get("program_name"));
                formatted.put("programId", intern.get("program_id"));
                formatted.put("universityName", intern.get("university_name"));
                formatted.put("majorName", intern.get("major_name"));
                formattedInterns.add(formatted);
            }

            return Map.of(
                    "success", true,
                    "data", formattedInterns,
                    "total", formattedInterns.size(),
                    "message", "Danh sách thực tập sinh trong các program bạn quản lý"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách thực tập sinh: " + e.getMessage()
            );
        }
    }

    // 2. Tạo intern profile mới
    @PostMapping("")
    public ResponseEntity<?> createProfile(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = internProfileService.createProfile(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thêm thất bại: " + e.getMessage()));
        }
    }

    // 3. Cập nhật intern profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = internProfileService.updateProfile(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // 4. Xóa intern profile
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            internProfileService.deleteProfile(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa thất bại: " + e.getMessage()));
        }
    }

    // 5. Thống kê số lượng intern theo trạng thái
    @GetMapping("/stats/status")
    public ResponseEntity<?> getInternStatusStats() {
        try {
            String sql = """
                    SELECT status, COUNT(*) AS count
                    FROM intern_profiles
                    GROUP BY status
                    """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi thống kê trạng thái thực tập sinh: " + e.getMessage()));
        }
    }


    @GetMapping("/programs")
    public ResponseEntity<?> getAllPrograms() {
        try {
            String sql = """
                    SELECT DISTINCT title 
                    FROM intern_programs 
                    WHERE title IS NOT NULL 
                    ORDER BY title
                    """;

            List<String> programs = jdbcTemplate.queryForList(sql, String.class);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", programs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải danh sách chương trình: " + e.getMessage()));
        }
    }
}