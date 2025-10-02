package com.example.be.controller;

import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    // 1. Lấy danh sách tất cả intern programs với filter
    @GetMapping("")
    public ResponseEntity<?> getAllInternships(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT p.program_id, p.title, p.capacity, p.start_date, p.end_date, 
                       p.description, p.created_by,
                       u.fullname as creator_name, u.email as creator_email,
                       COUNT(i.intern_id) as intern_count
                FROM intern_programs p
                LEFT JOIN users u ON p.created_by = u.user_id
                LEFT JOIN intern_profiles i ON p.program_id = i.program_id
                WHERE 1=1
                """);

            List<Object> params = new java.util.ArrayList<>();

            // Tìm kiếm theo title
            if (!query.trim().isEmpty()) {
                sql.append(" AND p.title LIKE ?");
                params.add("%" + query.trim() + "%");
            }

            // Lọc theo trạng thái (active/completed)
            if (status != null && !status.trim().isEmpty()) {
                if ("active".equalsIgnoreCase(status)) {
                    sql.append(" AND CURDATE() BETWEEN p.start_date AND p.end_date");
                } else if ("completed".equalsIgnoreCase(status)) {
                    sql.append(" AND p.end_date < CURDATE()");
                } else if ("upcoming".equalsIgnoreCase(status)) {
                    sql.append(" AND p.start_date > CURDATE()");
                }
            }

            sql.append("""
                GROUP BY p.program_id, p.title, p.capacity, p.start_date, p.end_date, 
                         p.description, p.created_by, u.fullname, u.email
                ORDER BY p.start_date DESC
                LIMIT ? OFFSET ?
                """);

            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> programs = jdbcTemplate.queryForList(
                    sql.toString(),
                    params.toArray()
            );

            // Đếm tổng số
            int total = getTotalProgramCount(query, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", programs,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", total,
                            "totalPages", (int) Math.ceil((double) total / size)
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải danh sách: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy chi tiết một intern program
    @GetMapping("/{id}")
    public ResponseEntity<?> getInternshipById(@PathVariable Long id) {
        try {
            String sql = """
                SELECT p.program_id, p.title, p.capacity, p.start_date, p.end_date, 
                       p.description, p.created_by,
                       u.fullname as creator_name, u.email as creator_email,
                       COUNT(i.intern_id) as intern_count
                FROM intern_programs p
                LEFT JOIN users u ON p.created_by = u.user_id
                LEFT JOIN intern_profiles i ON p.program_id = i.program_id
                WHERE p.program_id = ?
                GROUP BY p.program_id, p.title, p.capacity, p.start_date, p.end_date, 
                         p.description, p.created_by, u.fullname, u.email
                """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy chương trình với ID: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.get(0)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết: " + e.getMessage()
            ));
        }
    }

    // 3. Tạo intern program mới
    @PostMapping("")
    public ResponseEntity<?> createInternship(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            Integer capacity = (Integer) request.get("capacity");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String description = (String) request.get("description");
            Long createdBy = request.get("createdBy") != null
                    ? ((Number) request.get("createdBy")).longValue()
                    : null;

            // Validate
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập tên chương trình"
                ));
            }

            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng chọn thời gian bắt đầu và kết thúc"
                ));
            }

            // Insert vào database
            String sql = """
                INSERT INTO intern_programs (title, capacity, start_date, end_date, description, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql,
                    title.trim(),
                    capacity,
                    startDate,
                    endDate,
                    description != null ? description.trim() : null,
                    createdBy
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo chương trình thực tập thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tạo chương trình thất bại: " + e.getMessage()
            ));
        }
    }

    // 4. Cập nhật intern program
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInternship(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            // Kiểm tra program tồn tại
            String checkSql = "SELECT COUNT(*) FROM intern_programs WHERE program_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy chương trình với ID: " + id
                ));
            }

            String title = (String) request.get("title");
            Integer capacity = (Integer) request.get("capacity");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String description = (String) request.get("description");

            String sql = """
                UPDATE intern_programs 
                SET title = ?, 
                    capacity = ?, 
                    start_date = ?, 
                    end_date = ?, 
                    description = ?
                WHERE program_id = ?
                """;

            jdbcTemplate.update(sql,
                    title,
                    capacity,
                    startDate,
                    endDate,
                    description,
                    id
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật chương trình thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cập nhật thất bại: " + e.getMessage()
            ));
        }
    }

    // 5. Xóa intern program
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInternship(@PathVariable Long id) {
        try {
            // Kiểm tra có intern nào đang tham gia không
            String checkInternsSql = "SELECT COUNT(*) FROM intern_profiles WHERE program_id = ?";
            int internCount = jdbcTemplate.queryForObject(checkInternsSql, Integer.class, id);

            if (internCount > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không thể xóa chương trình có thực tập sinh đang tham gia! Hiện có " + internCount + " thực tập sinh."
                ));
            }

            // Xóa program
            String sql = "DELETE FROM intern_programs WHERE program_id = ?";
            int affected = jdbcTemplate.update(sql, id);

            if (affected == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy chương trình với ID: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa chương trình thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa thất bại: " + e.getMessage()
            ));
        }
    }

    // 6. Thống kê intern programs
    @GetMapping("/stats")
    public ResponseEntity<?> getInternshipStats() {
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_programs,
                    SUM(CASE WHEN CURDATE() BETWEEN start_date AND end_date THEN 1 ELSE 0 END) as active_programs,
                    SUM(CASE WHEN end_date < CURDATE() THEN 1 ELSE 0 END) as completed_programs,
                    SUM(CASE WHEN start_date > CURDATE() THEN 1 ELSE 0 END) as upcoming_programs,
                    SUM(capacity) as total_capacity,
                    COUNT(DISTINCT i.intern_id) as total_interns
                FROM intern_programs p
                LEFT JOIN intern_profiles i ON p.program_id = i.program_id
                """;

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    // Helper: Đếm tổng số programs
    private int getTotalProgramCount(String query, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM intern_programs p WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (!query.trim().isEmpty()) {
            sql.append(" AND p.title LIKE ?");
            params.add("%" + query.trim() + "%");
        }

        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status)) {
                sql.append(" AND CURDATE() BETWEEN p.start_date AND p.end_date");
            } else if ("completed".equalsIgnoreCase(status)) {
                sql.append(" AND p.end_date < CURDATE()");
            } else if ("upcoming".equalsIgnoreCase(status)) {
                sql.append(" AND p.start_date > CURDATE()");
            }
        }

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }
}