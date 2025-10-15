package com.example.be.controller;

import com.example.be.repository.UserRepository;
import com.example.be.service.CloudinaryRestService;
import com.example.be.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CVController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final CloudinaryRestService cloudinaryRestService;
    private final EmailService emailService;
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

    // 2. Lấy CV của chính người dùng dựa vào email
    @GetMapping("/my")
    public ResponseEntity<?> getMyCVs(@RequestParam("email") String email) {
        try {
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu tham số email"
                ));
            }

             // Tìm user_id từ email
             String findUserSql = "SELECT user_id FROM users WHERE email = ?";
             List<Map<String, Object>> userResult = jdbcTemplate.queryForList(findUserSql, email);
             
             if (userResult.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", List.of(),
                        "total", 0,
                         "message", "Không tìm thấy user với email: " + email
                ));
            }
            
             Long userId = ((Number) userResult.get(0).get("user_id")).longValue();

             // Lấy TẤT CẢ CV của user (cả approved và pending)
            String sql = """
                 SELECT file_id as cv_id, filename, file_type, status, uploaded_by, upload_at as uploaded_at, storage_path, intern_id
                FROM cv
                 WHERE user_id = ?
                ORDER BY file_id DESC
                """;

            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(sql, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", cvs,
                    "total", cvs.size(),
                    "userId", userId
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
                SELECT c.file_id as cv_id, c.file_type, c.status, c.uploaded_by, c.upload_at as uploaded_at, c.storage_path, c.filename,
                        c.user_id, usr.fullname as intern_name, usr.email as intern_email
                FROM cv c
                 JOIN users usr ON c.user_id = usr.user_id
                 WHERE c.status = 'PENDING' AND c.intern_id IS NULL
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
                
                 // Không tạo intern_profile khi upload CV, để NULL
                 finalInternId = null;
            }
            
            // finalInternId có thể null - CV sẽ được cấp intern_id khi approve

            // Upload file lên Cloudinary
            String fileName = file.getOriginalFilename();
            String fileType = contentType.contains("pdf") ? "application/pdf" : "application/msword";
            
            String cloudinaryResponse = cloudinaryRestService.uploadFile(file);
            JsonNode json = objectMapper.readTree(cloudinaryResponse);
            String fileUrl = json.get("secure_url").asText(); // URL file trên Cloudinary

            // Insert vào database với user_id vào cột uploaded_by và upload_at
            String insertSql = """
                INSERT INTO cv (intern_id, user_id, filename, file_type, status, storage_path, uploaded_by, upload_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?, NOW())
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

    // 8. HR initiates CV approval (sets status to ACCEPTING)
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptCV(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
         try {
             // Check CV exists and get details
             String checkSql = """
                 SELECT c.*, u.fullname as intern_name, u.email as intern_email
                 FROM cv c
                 JOIN users u ON c.user_id = u.user_id
                 WHERE c.file_id = ?
                 """;
             List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

             if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV không tồn tại"
                ));
            }

             Map<String, Object> cvData = result.get(0);
             String currentStatus = (String) cvData.get("status");
             
             // Only allow ACCEPTING from PENDING status
             if (!"PENDING".equals(currentStatus)) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV phải ở trạng thái PENDING để có thể duyệt"
                 ));
             }

             // Update status to ACCEPTING
             String updateSql = "UPDATE cv SET status = 'ACCEPTING' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã được đánh dấu duyệt! Bấm xác nhận để hoàn tất.",
                    "nextStep", "confirm"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 8.1. HR confirms CV approval (sets status to APPROVED and sends email)
    @PutMapping("/{id}/confirm-approve")
    public ResponseEntity<?> confirmApproveCV(@PathVariable Long id) {
        try {
             // Check CV exists and get details
             String checkSql = """
                 SELECT c.*, u.fullname as intern_name, u.email as intern_email
                 FROM cv c
                 JOIN users u ON c.user_id = u.user_id
                 WHERE c.file_id = ?
                 """;
             List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

             if (result.isEmpty()) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV không tồn tại"
                 ));
             }

             Map<String, Object> cvData = result.get(0);
             String currentStatus = (String) cvData.get("status");
             
             // Only allow APPROVED from ACCEPTING status
             if (!"ACCEPTING".equals(currentStatus)) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV phải ở trạng thái ACCEPTING để có thể xác nhận duyệt"
                 ));
             }

             // Get intern_id if user already has intern_profile (created by Admin manually)
             Long userId = ((Number) cvData.get("user_id")).longValue();
             String userEmail = (String) cvData.get("intern_email");
             
             Long internId = null;
             try {
                 // Check if user already has intern_profile
                 String checkInternSql = "SELECT intern_id FROM intern_profiles WHERE email = ?";
                 internId = jdbcTemplate.queryForObject(checkInternSql, Long.class, userEmail);
             } catch (Exception ex) {
                 // Không tự động tạo intern_profile - Admin phải tạo thủ công
                 System.out.println("⚠️ User chưa có intern_profile. Admin cần tạo thủ công trong trang 'Thêm thực tập'");
             }
             
             // Update CV with intern_id and APPROVED status
             String updateSql = "UPDATE cv SET status = 'APPROVED', intern_id = ? WHERE file_id = ?";
             jdbcTemplate.update(updateSql, internId, id);

            // Send approval email to intern
            String internEmail = (String) cvData.get("intern_email");
            String internName = (String) cvData.get("intern_name");
            String cvFileName = (String) cvData.get("filename");
            
            // Debug logging
            System.out.println("🔍 CV Approval Debug:");
            System.out.println("  - CV Data: " + cvData);
            System.out.println("  - Intern Email: " + internEmail);
            System.out.println("  - Intern Name: " + internName);
            System.out.println("  - CV File Name: " + cvFileName);
            
            if (internEmail != null && !internEmail.trim().isEmpty()) {
                try {
                    emailService.sendCVApprovalEmail(internEmail, internName, cvFileName);
                } catch (Exception emailError) {
                    System.err.println("Failed to send approval email: " + emailError.getMessage());
                    // Don't fail the operation if email fails
                }
            } else {
                System.err.println("❌ No intern email found in cvData!");
                // Try to get email from user table if intern_email is null
                try {
                    String fallbackSql = """
                        SELECT u.email FROM users u 
                        WHERE u.user_id = ?
                        """;
                    List<Map<String, Object>> userResult = jdbcTemplate.queryForList(fallbackSql, userId);
                    if (!userResult.isEmpty()) {
                        String fallbackEmail = (String) userResult.get(0).get("email");
                        System.out.println("🔄 Found fallback email: " + fallbackEmail);
                        emailService.sendCVApprovalEmail(fallbackEmail, internName, cvFileName);
                    }
                } catch (Exception fallbackError) {
                    System.err.println("Fallback email lookup failed: " + fallbackError.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã được duyệt thành công! Email thông báo đã được gửi đến " + internEmail
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 9. HR initiates CV rejection (sets status to REJECTING)
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectCV(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        try {
            String rejectionReason = request != null ? request.get("reason") : "";
            
             // Check CV exists and get details
             String checkSql = """
                 SELECT c.*, u.fullname as intern_name, u.email as intern_email
                 FROM cv c
                 JOIN users u ON c.user_id = u.user_id
                 WHERE c.file_id = ?
                 """;
             List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

             if (result.isEmpty()) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV không tồn tại"
                 ));
             }

             Map<String, Object> cvData = result.get(0);
             String currentStatus = (String) cvData.get("status");
             
             // Only allow REJECTING from PENDING status
             if (!"PENDING".equals(currentStatus)) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV phải ở trạng thái PENDING để có thể từ chối"
                 ));
             }

             // Update status to REJECTING and store reason
             String updateSql = "UPDATE cv SET status = 'REJECTING' WHERE file_id = ?";
             jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã được đánh dấu từ chối! Bấm xác nhận để hoàn tất.",
                    "nextStep", "confirm",
                    "reason", rejectionReason
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // 9.1. HR confirms CV rejection (sets status to REJECTED and sends email)
    @PutMapping("/{id}/confirm-reject")
    public ResponseEntity<?> confirmRejectCV(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("reason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                String updateSql = "UPDATE cv SET status = 'PENDING' WHERE file_id = ?";
                jdbcTemplate.update(updateSql, id);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập lý do từ chối"
                ));
            }

             // Check CV exists and get details
             String checkSql = """
                 SELECT c.*, u.fullname as intern_name, u.email as intern_email
                 FROM cv c
                 JOIN users u ON c.user_id = u.user_id
                 WHERE c.file_id = ?
                 """;
             List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

             if (result.isEmpty()) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV không tồn tại"
                 ));
             }

             Map<String, Object> cvData = result.get(0);
             String currentStatus = (String) cvData.get("status");
             
             // Only allow REJECTED from REJECTING status
             if (!"REJECTING".equals(currentStatus)) {
                 return ResponseEntity.badRequest().body(Map.of(
                         "success", false,
                         "message", "CV phải ở trạng thái REJECTING để có thể xác nhận từ chối"
                 ));
             }

             // Update status to REJECTED
             String updateSql = "UPDATE cv SET status = 'REJECTED' WHERE file_id = ?";
             jdbcTemplate.update(updateSql, id);

            // Send rejection email to intern
            String internEmail = (String) cvData.get("intern_email");
            String internName = (String) cvData.get("intern_name");
            String cvFileName = (String) cvData.get("filename");
            
            if (internEmail != null && !internEmail.trim().isEmpty()) {
                try {
                    emailService.sendCVRejectionEmail(internEmail, internName, cvFileName, rejectionReason);
                } catch (Exception emailError) {
                    System.err.println("Failed to send rejection email: " + emailError.getMessage());
                    // Don't fail the operation if email fails
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã bị từ chối! Email thông báo đã được gửi đến " + internEmail,
                    "reason", rejectionReason
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // Legacy endpoint for backward compatibility
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

            // Update status to APPROVED (legacy behavior)
            String updateSql = "UPDATE cv SET status = 'APPROVED' WHERE file_id = ?";
            jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "CV đã được duyệt thành công! (Legacy endpoint)"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
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
