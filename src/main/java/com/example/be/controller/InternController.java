package com.example.be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
public class InternController {

    private final JdbcTemplate jdbcTemplate;

    // 1. Tìm kiếm và lọc thực tập sinh
    @GetMapping("")
    public ResponseEntity<?> searchInterns(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "university", required = false) String university,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "yearOfStudy", required = false) Integer yearOfStudy,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            // Build base query với JOIN
            StringBuilder sql = new StringBuilder("""
                SELECT i.intern_id, i.fullname, i.dob, i.major_id, i.year_of_study, i.phone, i.available_from,
                       u.name_uni as university_name,
                       p.title as program_title, p.start_date as program_start, p.end_date as program_end,
                       COUNT(d.document_id) as document_count,
                       SUM(CASE WHEN d.status = 'PENDING' THEN 1 ELSE 0 END) as pending_docs,
                       SUM(CASE WHEN d.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_docs
                FROM intern_profiles i
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                LEFT JOIN intern_documents d ON i.intern_id = d.intern_id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            // Thêm điều kiện tìm kiếm theo tên và email
            if (!query.trim().isEmpty()) {
                sql.append(" AND (i.fullname LIKE ? OR i.phone LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // Lọc theo trường
            if (university != null && !university.trim().isEmpty()) {
                sql.append(" AND u.name_uni LIKE ?");
                params.add("%" + university.trim() + "%");
            }

            // Lọc theo ngành (major_id)
            if (major != null && !major.trim().isEmpty()) {
                sql.append(" AND i.major_id = ?");
                params.add(Integer.parseInt(major));
            }

            // Lọc theo chương trình thực tập
            if (program != null && !program.trim().isEmpty()) {
                sql.append(" AND p.title LIKE ?");
                params.add("%" + program.trim() + "%");
            }

            // Lọc theo năm học
            if (yearOfStudy != null && yearOfStudy > 0) {
                sql.append(" AND i.year_of_study = ?");
                params.add(yearOfStudy);
            }

            // Group by để tránh duplicate
            sql.append("""
                GROUP BY i.intern_id, i.fullname, i.dob, i.major_id, i.year_of_study, i.phone, i.available_from,
                         u.name_uni, p.title, p.start_date, p.end_date
                ORDER BY i.fullname ASC
                """);

            // Thêm LIMIT và OFFSET cho pagination
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> interns = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            // Đếm tổng số record cho pagination
            int totalCount = getTotalInternCount(query, university, major, program, yearOfStudy);

            // Tính toán thông tin pagination
            int totalPages = (int) Math.ceil((double) totalCount / size);
            boolean hasNext = (page + 1) < totalPages;
            boolean hasPrevious = page > 0;

            // Format response
            var response = interns.stream()
                    .map(this::formatInternResponse)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", totalCount,
                            "totalPages", totalPages,
                            "hasNext", hasNext,
                            "hasPrevious", hasPrevious
                    ),
                    "message", response.isEmpty() ? "Không có kết quả phù hợp" : "Tìm thấy " + totalCount + " thực tập sinh"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tìm kiếm thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy chi tiết một thực tập sinh
    @GetMapping("/{id}")
    public ResponseEntity<?> getInternById(@PathVariable Long id) {
        try {
            String sql = """
                SELECT i.intern_id, i.fullname, i.dob, i.major_id, i.year_of_study, i.phone, i.available_from,
                       u.name_uni as university_name,
                       p.title as program_title, p.start_date as program_start, p.end_date as program_end,
                       p.description as program_description
                FROM intern_profiles i
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                WHERE i.intern_id = ?
                """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy thực tập sinh với ID: " + id
                ));
            }

            // Lấy thêm thông tin tài liệu
            String docSql = """
                SELECT document_id, document_type, status, uploaded_at, file_detail, rejection_reason
                FROM intern_documents 
                WHERE intern_id = ?
                ORDER BY uploaded_at DESC
                """;

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, id);

            Map<String, Object> intern = formatInternDetailResponse(result.get(0));
            intern.put("documents", documents);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intern
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 3. Thống kê thực tập sinh
    @GetMapping("/stats")
    public ResponseEntity<?> getInternStats() {
        try {
            // Thống kê tổng quan
            String generalSql = """
                SELECT 
                    COUNT(*) as total_interns,
                    COUNT(CASE WHEN CURDATE() BETWEEN COALESCE(p.start_date, '1900-01-01') 
                                                 AND COALESCE(p.end_date, '2100-01-01') THEN 1 END) as active_interns,
                    AVG(i.year_of_study) as avg_year_of_study
                FROM intern_profiles i
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                """;

            Map<String, Object> generalStats = jdbcTemplate.queryForMap(generalSql);

            // Thống kê theo trường
            String universitySql = """
                SELECT u.name_uni as university, COUNT(*) as count
                FROM intern_profiles i
                JOIN universities u ON i.uni_id = u.uni_id
                GROUP BY u.uni_id, u.name_uni
                ORDER BY count DESC
                LIMIT 10
                """;

            List<Map<String, Object>> universityStats = jdbcTemplate.queryForList(universitySql);

            // Thống kê theo năm học
            String yearSql = """
                SELECT year_of_study, COUNT(*) as count
                FROM intern_profiles 
                WHERE year_of_study IS NOT NULL
                GROUP BY year_of_study
                ORDER BY year_of_study
                """;

            List<Map<String, Object>> yearStats = jdbcTemplate.queryForList(yearSql);

            // Thống kê tài liệu
            String docSql = """
                SELECT 
                    COUNT(DISTINCT i.intern_id) as interns_with_docs,
                    COUNT(d.document_id) as total_documents,
                    AVG(doc_per_intern.doc_count) as avg_docs_per_intern
                FROM intern_profiles i
                LEFT JOIN intern_documents d ON i.intern_id = d.intern_id
                LEFT JOIN (
                    SELECT intern_id, COUNT(*) as doc_count 
                    FROM intern_documents 
                    GROUP BY intern_id
                ) doc_per_intern ON i.intern_id = doc_per_intern.intern_id
                """;

            Map<String, Object> docStats = jdbcTemplate.queryForMap(docSql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "general", generalStats,
                            "byUniversity", universityStats,
                            "byYear", yearStats,
                            "documents", docStats
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy danh sách trường để filter
    @GetMapping("/universities")
    public ResponseEntity<?> getUniversities() {
        try {
            String sql = """
                SELECT DISTINCT u.uni_id, u.name_uni, COUNT(i.intern_id) as intern_count
                FROM universities u
                LEFT JOIN intern_profiles i ON u.uni_id = i.uni_id
                GROUP BY u.uni_id, u.name_uni
                HAVING intern_count > 0
                ORDER BY u.name_uni
                """;

            List<Map<String, Object>> universities = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", universities
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách trường: " + e.getMessage()
            ));
        }
    }

    // 5. Lấy danh sách ngành để filter
    @GetMapping("/majors")
    public ResponseEntity<?> getMajors() {
        try {
            // Return static majors for now (you can create majors table later)
            List<Map<String, Object>> majors = List.of(
                    Map.of("id", 1, "name", "Công nghệ thông tin"),
                    Map.of("id", 2, "name", "Khoa học máy tính"),
                    Map.of("id", 3, "name", "Kỹ thuật điện tử"),
                    Map.of("id", 4, "name", "Kỹ thuật phần mềm"),
                    Map.of("id", 5, "name", "An toàn thông tin")
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", majors
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách ngành: " + e.getMessage()
            ));
        }
    }
    private int getTotalInternCount(String query, String university, String major, String program, Integer yearOfStudy) {
        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(DISTINCT i.intern_id)
            FROM intern_profiles i
            LEFT JOIN universities u ON i.uni_id = u.uni_id
            LEFT JOIN intern_programs p ON i.program_id = p.program_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (!query.trim().isEmpty()) {
            countSql.append(" AND (i.fullname LIKE ? OR i.phone LIKE ?)");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (university != null && !university.trim().isEmpty()) {
            countSql.append(" AND u.name_uni LIKE ?");
            params.add("%" + university.trim() + "%");
        }

        if (major != null && !major.trim().isEmpty()) {
            countSql.append(" AND i.major_id = ?");
            params.add(Integer.parseInt(major));
        }

        if (program != null && !program.trim().isEmpty()) {
            countSql.append(" AND p.title LIKE ?");
            params.add("%" + program.trim() + "%");
        }

        if (yearOfStudy != null && yearOfStudy > 0) {
            countSql.append(" AND i.year_of_study = ?");
            params.add(yearOfStudy);
        }

        return jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());
    }

    // Helper method: Format response cho danh sách (khớp với FE)
    private Map<String, Object> formatInternResponse(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", row.get("intern_id"));
        result.put("student", row.get("fullname") != null ? row.get("fullname") : "");
        result.put("studentEmail", row.get("student_email") != null ? row.get("student_email") : "");
        result.put("school", row.get("university_name") != null ? row.get("university_name") : "");
        result.put("major", getMajorName(row.get("major_id")));
        result.put("phone", row.get("phone") != null ? row.get("phone") : "");
        result.put("dateOfBirth", row.get("dob") != null ? row.get("dob").toString() : "");
        result.put("yearOfStudy", row.get("year_of_study") != null ? row.get("year_of_study") : 0);
        result.put("availableFrom", row.get("available_from") != null ? row.get("available_from").toString() : "");
        result.put("program", row.get("program_title") != null ? row.get("program_title") : "N/A");
        result.put("majorId", row.get("major_id"));
        result.put("status", "active");

        Map<String, Object> docStats = new HashMap<>();
        docStats.put("total", row.get("document_count") != null ? row.get("document_count") : 0);
        docStats.put("pending", row.get("pending_docs") != null ? row.get("pending_docs") : 0);
        docStats.put("approved", row.get("approved_docs") != null ? row.get("approved_docs") : 0);
        result.put("documentStats", docStats);

        return result;
    }

    // Helper: Generate email from name (for FE compatibility)
    private String generateEmailFromName(Object name) {
        if (name == null) return "";
        String cleanName = name.toString().toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");
        return cleanName.isEmpty() ? "intern@example.com" : cleanName + "@gmail.com";
    }

    // Helper: Map major_id to major name (you can expand this)
    private String getMajorName(Object majorId) {
        if (majorId == null) return "";

        // TODO: You can create a major mapping table or query
        // For now, return simple mapping
        int id = (Integer) majorId;
        return switch (id) {
            case 1 -> "Công nghệ thông tin";
            case 2 -> "Khoa học máy tính";
            case 3 -> "Kỹ thuật điện tử";
            case 4 -> "Kỹ thuật phần mềm";
            case 5 -> "An toàn thông tin";
            default -> "Chuyên ngành khác";
        };
    }

    // Helper method: Format response cho chi tiết
    private Map<String, Object> formatInternDetailResponse(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", row.get("intern_id"));
        result.put("fullName", row.get("fullname") != null ? row.get("fullname") : "");
        result.put("email", row.get("student_email") != null ? row.get("student_email") : "");
        result.put("phone", row.get("phone") != null ? row.get("phone") : "");
        result.put("dateOfBirth", row.get("dob") != null ? row.get("dob").toString() : "");
        result.put("yearOfStudy", row.get("year_of_study") != null ? row.get("year_of_study") : 0);
        result.put("availableFrom", row.get("available_from") != null ? row.get("available_from").toString() : "");
        result.put("majorId", row.get("major_id"));
        result.put("university", row.get("university_name") != null ? row.get("university_name") : "N/A");

        Map<String, Object> program = new HashMap<>();
        program.put("title", row.get("program_title") != null ? row.get("program_title") : "N/A");
        program.put("startDate", row.get("program_start") != null ? row.get("program_start").toString() : "");
        program.put("endDate", row.get("program_end") != null ? row.get("program_end").toString() : "");
        program.put("description", row.get("program_description") != null ? row.get("program_description") : "");
        result.put("program", program);

        return result;
    }
}