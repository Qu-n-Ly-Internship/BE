package com.example.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternshipService {
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getAllInternships(String query, String startDate, String endDate, int page, int size) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT p.*, COUNT(DISTINCT i.intern_id) as intern_count,
                       GROUP_CONCAT(DISTINCT i.fullname) as intern_names
                FROM intern_programs p
                LEFT JOIN intern_profiles i ON p.program_id = i.program_id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                sql.append(" AND (p.title LIKE ? OR p.description LIKE ?)");
                String pattern = "%" + query.trim() + "%";
                params.add(pattern);
                params.add(pattern);
            }

            if (startDate != null && !startDate.trim().isEmpty()) {
                sql.append(" AND p.start_date >= ?");
                params.add(startDate);
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                sql.append(" AND p.end_date <= ?");
                params.add(endDate);
            }

            sql.append(" GROUP BY p.program_id ORDER BY p.start_date DESC");
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> programs = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            return Map.of(
                "success", true,
                "data", programs,
                "total", getTotalProgramCount(query, startDate, endDate)
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách chương trình: " + e.getMessage());
        }
    }

    public Map<String, Object> getProgramById(Long id) {
        try {
            String sql = """
                SELECT p.*, COUNT(DISTINCT i.intern_id) as intern_count,
                       GROUP_CONCAT(DISTINCT i.fullname) as intern_names
                FROM intern_programs p
                LEFT JOIN intern_profiles i ON p.program_id = i.program_id
                WHERE p.program_id = ?
                GROUP BY p.program_id
                """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (result.isEmpty()) {
                throw new RuntimeException("Không tìm thấy chương trình với ID: " + id);
            }

            return Map.of("success", true, "data", result.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy chi tiết chương trình: " + e.getMessage());
        }
    }

    public Map<String, Object> createProgram(Map<String, Object> request) {
        try {
            validateProgramRequest(request);

            String sql = """
                INSERT INTO intern_programs (title, description, capacity, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql,
                request.get("title"),
                request.get("description"),
                request.get("capacity"),
                request.get("startDate"),
                request.get("endDate")
            );

            return Map.of("success", true, "message", "Tạo chương trình thành công!");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chương trình: " + e.getMessage());
        }
    }

    public Map<String, Object> updateProgram(Long id, Map<String, Object> request) {
        try {
            validateProgramRequest(request);

            if (!programExists(id)) {
                throw new RuntimeException("Không tìm thấy chương trình với ID: " + id);
            }

            String sql = """
                UPDATE intern_programs
                SET title = ?,
                    description = ?,
                    capacity = ?,
                    start_date = ?,
                    end_date = ?
                WHERE program_id = ?
                """;

            jdbcTemplate.update(sql,
                request.get("title"),
                request.get("description"),
                request.get("capacity"),
                request.get("startDate"),
                request.get("endDate"),
                id
            );

            return Map.of("success", true, "message", "Cập nhật chương trình thành công!");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật chương trình: " + e.getMessage());
        }
    }

    public void deleteProgram(Long id) {
        try {
            if (!programExists(id)) {
                throw new RuntimeException("Không tìm thấy chương trình với ID: " + id);
            }

            // Check if program has interns
            String checkSql = "SELECT COUNT(*) FROM intern_profiles WHERE program_id = ?";
            int internCount = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (internCount > 0) {
                throw new RuntimeException("Không thể xóa chương trình đang có thực tập sinh!");
            }

            jdbcTemplate.update("DELETE FROM intern_programs WHERE program_id = ?", id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa chương trình: " + e.getMessage());
        }
    }

    public Map<String, Object> getInternsByProgram(Long programId) {
        try {
            String sql = """
                SELECT i.*, u.name_uni as university_name
                FROM intern_profiles i
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                WHERE i.program_id = ?
                ORDER BY i.fullname
                """;

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql, programId);

            return Map.of("success", true, "data", interns, "total", interns.size());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách thực tập sinh: " + e.getMessage());
        }
    }

    private int getTotalProgramCount(String query, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM intern_programs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (title LIKE ? OR description LIKE ?)");
            String pattern = "%" + query.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND start_date >= ?");
            params.add(startDate);
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND end_date <= ?");
            params.add(endDate);
        }

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    private void validateProgramRequest(Map<String, Object> request) {
        String title = (String) request.get("title");
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề không được để trống");
        }

        Integer capacity = (Integer) request.get("capacity");
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("Số lượng tối đa phải lớn hơn 0");
        }

        String startDate = (String) request.get("startDate");
        String endDate = (String) request.get("endDate");
        if (startDate == null || endDate == null ||
            startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống");
        }
    }

    private boolean programExists(Long id) {
        String sql = "SELECT COUNT(*) FROM intern_programs WHERE program_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, id) > 0;
    }
}
