package com.example.be.controller;

import com.example.be.repository.UserRepository;
import com.example.be.service.CloudinaryRestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CVController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final CloudinaryRestService cloudinaryRestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Lấy tất cả CV với filter
    @GetMapping("")
    public ResponseEntity<?> getAllCVs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", defaultValue = "") String query
    ) {
        try {
            String sql = """
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.storage_path, c.filename,
                       i.intern_id, i.fullname as intern_name, i.phone,
                       u.name_uni as university_name
                FROM cv c
                JOIN intern_profiles i ON c.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                WHERE 1=1
                """;

            if (status != null && !status.isBlank()) {
                sql += " AND c.status = '" + status + "'";
            }
            if (query != null && !query.isBlank()) {
                sql += " AND (i.fullname LIKE '%" + query + "%' OR i.phone LIKE '%" + query + "%')";
            }

            sql += " ORDER BY c.file_id DESC";

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách CV: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy CV của chính người dùng dựa vào email của họ
    @GetMapping("/my")
    public ResponseEntity<?> getMyCVs(@RequestParam("email") String email) {
        try {
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu tham số email"
                ));
            }

            // Tìm intern_id từ email
            String findInternSql = "SELECT intern_id FROM intern_profiles WHERE email = ?";
            List<Map<String, Object>> internResult = jdbcTemplate.queryForList(findInternSql, email);
            
            if (internResult.isEmpty()) {
                // Không tìm thấy intern_profile với email này
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", List.of(),
                        "total", 0,
                        "message", "Không tìm thấy hồ sơ thực tập với email: " + email
                ));
            }
            
            Long internId = ((Number) internResult.get(0).get("intern_id")).longValue();

            // Lấy TẤT CẢ CV của intern
            String sql = """
                SELECT file_id as cv_id, filename, file_type, status, uploaded_by, storage_path
                FROM cv
                WHERE intern_id = ?
                ORDER BY file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql, internId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size(),
                    "internId", internId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV của bạn: " + e.getMessage()
            ));
        }
    }

    // 3. Lấy CV chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCVs() {
        try {
            String sql = """
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.storage_path, c.filename,
                       i.intern_id, i.fullname as intern_name, i.phone,
                       u.name_uni as university_name
                FROM cv c
                LEFT JOIN intern_profiles i ON c.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                WHERE c.status = 'PENDING'
                ORDER BY c.file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV chờ duyệt: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy chi tiết một CV
    @GetMapping("/{id}")
    public ResponseEntity<?> getCVById(@PathVariable Long id) {
        try {
            String sql = """
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.storage_path, c.filename,
                       i.intern_id, i.fullname as intern_name, i.phone, i.dob, i.year_of_study,
                       u.name_uni as university_name,
                       p.title as program_title
                FROM cv c
                JOIN intern_profiles i ON c.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON i.program_id = p.program_id
                WHERE c.file_id = ?
                """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy CV với ID: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.get(0)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết CV: " + e.getMessage()
            ));
        }
    }

    // 5. Lấy CV theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getCVsByIntern(@PathVariable Long internId) {
        try {
            String sql = """
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.storage_path, c.filename,
                       i.intern_id, i.fullname as intern_name, i.phone,
                       u.name_uni as university_name
                FROM cv c
                JOIN intern_profiles i ON c.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                WHERE c.intern_id = ?
                ORDER BY c.file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql, internId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV của thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 6. Thống kê CV theo trạng thái
    @GetMapping("/stats")
    public ResponseEntity<?> getCVStats() {
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
                    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected
                FROM cv
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

    // 7. Upload CV mới
    @PostMapping("/upload")
    public ResponseEntity<?> uploadCV(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "internId", required = false) Long internId,
            @RequestParam(value = "uploaderEmail", required = false) String uploaderEmail
    ) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File không được để trống"
                ));
            }

            // Check file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File không được vượt quá 10MB"
                ));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("pdf") &&
                    !contentType.contains("msword") &&
                    !contentType.contains("wordprocessingml"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Chỉ hỗ trợ file PDF và Word cho CV"
                ));
            }

            // Tìm hoặc tạo intern_profile và lấy user_id
            Long finalInternId = internId;
            Long userId = null;
            
            if (finalInternId == null && uploaderEmail != null && !uploaderEmail.isBlank()) {
                var userOpt = userRepository.findByEmail(uploaderEmail.trim());
                if (userOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Không tìm thấy user với email: " + uploaderEmail
                    ));
                }
                
                var user = userOpt.get();
                userId = user.getId(); // Lấy user_id để lưu vào uploaded_by
                
                // Kiểm tra xem user đã có intern_profile chưa
                String checkInternSql = "SELECT intern_id FROM intern_profiles WHERE email = ? LIMIT 1";
                try {
                    finalInternId = jdbcTemplate.queryForObject(checkInternSql, Long.class, uploaderEmail.trim());
                } catch (Exception ex) {
                    // Chưa có intern_profile, tạo mới
                    String insertInternSql = """
                        INSERT INTO intern_profiles 
                        (fullname, email, uni_id, major_id, program_id, available_from, end_date, status, phone, year_of_study)
                        VALUES (?, ?, NULL, NULL, NULL, NULL, NULL, 'PENDING', '', 0)
                        """;
                    jdbcTemplate.update(insertInternSql, user.getFullName(), user.getEmail());
                    finalInternId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                }
            }
            
            if (finalInternId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không thể xác định intern_id. Vui lòng cung cấp internId hoặc uploaderEmail."
                ));
            }

            // Upload file lên Cloudinary
            String fileName = file.getOriginalFilename();
            String fileType = contentType.contains("pdf") ? "application/pdf" : "application/msword";
            
            String cloudinaryResponse = cloudinaryRestService.uploadFile(file);
            JsonNode json = objectMapper.readTree(cloudinaryResponse);
            String fileUrl = json.get("secure_url").asText(); // URL file trên Cloudinary

            // Insert vào database với user_id vào cột uploaded_by
            String insertSql = """
                INSERT INTO cv (intern_id, user_id, filename, file_type, status, storage_path, uploaded_by)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
                """;
            // Note: Bảng cv có primary key là file_id, không phải cv_id

            // userId có thể null nếu không có uploaderEmail
            Integer uploadedByValue = (userId != null) ? userId.intValue() : null;
            jdbcTemplate.update(insertSql, finalInternId, userId, fileName, fileType, fileUrl, uploadedByValue);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tải lên CV thành công! Chờ HR duyệt.",
                    "data", Map.of(
                            "fileName", fileName,
                            "fileType", fileType,
                            "size", file.getSize(),
                            "status", "PENDING"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tải lên CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 8. Duyệt CV
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCV(@PathVariable Long id) {
        try {
            // Check CV exists
            String checkSql = "SELECT COUNT(*) FROM cv WHERE file_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV không tồn tại"
                ));
            }

            // Update status to APPROVED
            String updateSql = "UPDATE cv SET status = 'APPROVED' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã được duyệt thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 9. Từ chối CV
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectCV(@PathVariable Long id) {
        try {
            // Check CV exists
            String checkSql = "SELECT COUNT(*) FROM cv WHERE file_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV không tồn tại"
                ));
            }

            // Update status to REJECTED
            String updateSql = "UPDATE cv SET status = 'REJECTED' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã bị từ chối!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 10. Xóa CV
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCV(@PathVariable Long id) {
        try {
            // Kiểm tra CV tồn tại
            String checkSql = "SELECT COUNT(*) FROM cv WHERE file_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV không tồn tại"
                ));
            }

            // Xóa CV
            String deleteSql = "DELETE FROM cv WHERE file_id = ?";
            jdbcTemplate.update(deleteSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa CV thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa CV thất bại: " + e.getMessage()
            ));
        }
    }
}
