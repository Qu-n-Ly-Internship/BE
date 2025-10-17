package com.example.be.service;

import com.example.be.service.CloudinaryRestService;
import com.example.be.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CVService {
    private final JdbcTemplate jdbcTemplate;
    private final CloudinaryRestService cloudinaryRestService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getAllCVs(String status, String query) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.storage_path, c.filename,
                       i.intern_id, i.fullname as intern_name, i.phone,
                       u.name_uni as university_name
                FROM cv c
                JOIN intern_profiles i ON c.intern_id = i.intern_id
                LEFT JOIN universities u ON i.uni_id = u.uni_id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                sql.append(" AND c.status = ?");
                params.add(status);
            }
            if (query != null && !query.isBlank()) {
                sql.append(" AND (i.fullname LIKE ? OR i.phone LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            sql.append(" ORDER BY c.file_id DESC");

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            return Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size()
            );

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách CV: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getMyCVs(String email) {
        try {
            if (email == null || email.isBlank()) {
                return Map.of("success", false, "message", "Thiếu tham số email");
            }

            String findUserSql = "SELECT user_id FROM users WHERE email = ?";
            List<Map<String, Object>> userResult = jdbcTemplate.queryForList(findUserSql, email);

            if (userResult.isEmpty()) {
                return Map.of(
                        "success", true,
                        "data", List.of(),
                        "total", 0,
                        "message", "Không tìm thấy user với email: " + email
                );
            }

            Long userId = ((Number) userResult.get(0).get("user_id")).longValue();

            String sql = """
                 SELECT file_id as cv_id, filename, file_type, status, uploaded_by, upload_at as uploaded_at, storage_path, intern_id
                FROM cv
                 WHERE user_id = ?
                ORDER BY file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql, userId);

            return Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size(),
                    "userId", userId
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV của bạn: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getPendingCVs() {
        try {
            String sql = """
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.upload_at as uploaded_at, c.storage_path, c.filename,
                        c.user_id, usr.fullname as intern_name, usr.email as intern_email
                FROM cv c
                 JOIN users usr ON c.user_id = usr.user_id
                 WHERE c.status = 'PENDING' AND c.intern_id IS NULL
                ORDER BY c.file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql);

            return Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size()
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy CV chờ duyệt: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getCVById(Long id) {
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
                return Map.of("success", false, "message", "Không tìm thấy CV với ID: " + id);
            }

            return Map.of("success", true, "data", result.get(0));

        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi lấy chi tiết CV: " + e.getMessage());
        }
    }

    public Map<String, Object> getCVsByIntern(Long internId) {
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

            return Map.of("success", true, "data", cvs, "total", cvs.size());

        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi lấy CV của thực tập sinh: " + e.getMessage());
        }
    }

    public Map<String, Object> getCVStats() {
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

            return Map.of("success", true, "data", stats);

        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File không được vượt quá 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.contains("pdf") &&
                !contentType.contains("msword") &&
                !contentType.contains("wordprocessingml"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file PDF và Word cho CV");
        }
    }

    private Long getUserIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        var userResult = jdbcTemplate.queryForList(
            "SELECT user_id, email FROM users WHERE email = ?",
            email.trim()
        );

        if (userResult.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với email: " + email);
        }

        return ((Number) userResult.get(0).get("user_id")).longValue();
    }

    private void sendCVEmail(String email, String name, String fileName, String template, Map<String, String> additionalParams) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("name", name);
            params.put("filename", fileName);
            if (additionalParams != null) {
                params.putAll(additionalParams);
            }

            emailService.sendEmailFromTemplate(email, template, params);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    private Map<String, Object> checkCVStatus(Long id, String expectedStatus) {
        String checkSql = """
             SELECT c.*, u.fullname as intern_name, u.email as intern_email
             FROM cv c
             JOIN users u ON c.user_id = u.user_id
             WHERE c.file_id = ?
             """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

        if (result.isEmpty()) {
            throw new RuntimeException("CV không tồn tại");
        }

        Map<String, Object> cvData = result.get(0);
        String currentStatus = (String) cvData.get("status");

        if (!expectedStatus.equals(currentStatus)) {
            throw new IllegalStateException("CV phải ở trạng thái " + expectedStatus);
        }

        return cvData;
    }

    public Map<String, Object> uploadCV(MultipartFile file, Long internId, String uploaderEmail) {
        try {
            validateFile(file);

            Long userId = getUserIdFromEmail(uploaderEmail);
            Long finalInternId = internId;

            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType().contains("pdf") ? "application/pdf" : "application/msword";

            String cloudinaryResponse = cloudinaryRestService.uploadFile(file);
            JsonNode json = objectMapper.readTree(cloudinaryResponse);
            String fileUrl = json.get("secure_url").asText();

            String insertSql = """
                INSERT INTO cv (intern_id, user_id, filename, file_type, status, storage_path, uploaded_by, upload_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?, NOW())
                """;

            jdbcTemplate.update(
                insertSql,
                finalInternId,
                userId,
                fileName,
                fileType,
                fileUrl,
                userId
            );

            return Map.of(
                    "success", true,
                    "message", "Tải lên CV thành công! Chờ HR duyệt.",
                    "data", Map.of(
                            "fileName", fileName,
                            "fileType", fileType,
                            "size", file.getSize(),
                            "status", "PENDING"
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("Tải lên CV thất bại: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> acceptCV(Long id) {
        try {
            Map<String, Object> cvData = checkCVStatus(id, "PENDING");

            String updateSql = "UPDATE cv SET status = 'ACCEPTING' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            return Map.of("success", true, "message", "CV đã được đánh dấu duyệt! Bấm xác nhận để hoàn tất.", "nextStep", "confirm");

        } catch (Exception e) {
            jdbcTemplate.update("UPDATE cv SET status = 'PENDING' WHERE file_id = ?", id);
            return Map.of("success", false, "message", "Duyệt CV thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> confirmApproveCV(Long id) {
        try {
            Map<String, Object> cvData = checkCVStatus(id, "ACCEPTING");

            String updateSql = "UPDATE cv SET status = 'APPROVED' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            String internEmail = (String) cvData.get("intern_email");
            String internName = (String) cvData.get("intern_name");
            String cvFileName = (String) cvData.get("filename");

            sendCVEmail(internEmail, internName, cvFileName, "CV_APPROVAL", null);

            return Map.of("success", true, "message", "CV đã được duyệt thành công! Email thông báo đã được gửi.");

        } catch (Exception e) {
            return Map.of("success", false, "message", "Xác nhận duyệt CV thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> rejectCV(Long id, String reason) {
        try {
            Map<String, Object> cvData = checkCVStatus(id, "PENDING");

            jdbcTemplate.update("UPDATE cv SET status = 'REJECTING' WHERE file_id = ?", id);

            return Map.of("success", true, "message", "CV đã được đánh dấu từ chối! Bấm xác nhận để hoàn tất.", "nextStep", "confirm", "reason", reason);

        } catch (Exception e) {
            return Map.of("success", false, "message", "Từ chối CV thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> confirmRejectCV(Long id, String reason) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                jdbcTemplate.update("UPDATE cv SET status = 'PENDING' WHERE file_id = ?", id);
                return Map.of("success", false, "message", "Vui lòng nhập lý do từ chối");
            }

            Map<String, Object> cvData = checkCVStatus(id, "REJECTING");

            jdbcTemplate.update("UPDATE cv SET status = 'REJECTED' WHERE file_id = ?", id);

            String internEmail = (String) cvData.get("intern_email");
            String internName = (String) cvData.get("intern_name");
            String cvFileName = (String) cvData.get("filename");

            sendCVEmail(internEmail, internName, cvFileName, "CV_REJECTION", Map.of("reason", reason));

            return Map.of("success", true, "message", "CV đã bị từ chối! Email thông báo đã được gửi đến " + internEmail, "reason", reason);

        } catch (Exception e) {
            return Map.of("success", false, "message", "Xác nhận từ chối CV thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> legacyApproveCV(Long id) {
        try {
            String checkSql = "SELECT COUNT(*) FROM cv WHERE file_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return Map.of("success", false, "message", "CV không tồn tại");
            }

            jdbcTemplate.update("UPDATE cv SET status = 'APPROVED' WHERE file_id = ?", id);

            return Map.of("success", true, "message", "CV đã được duyệt thành công! (Legacy endpoint)");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Duyệt CV thất bại: " + e.getMessage());
        }
    }

    public Map<String, Object> deleteCV(Long id) {
        try {
            String checkSql = "SELECT COUNT(*) FROM cv WHERE file_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return Map.of("success", false, "message", "CV không tồn tại");
            }

            jdbcTemplate.update("DELETE FROM cv WHERE file_id = ?", id);

            return Map.of("success", true, "message", "Đã xóa CV thành công!");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Xóa CV thất bại: " + e.getMessage());
        }
    }
}

