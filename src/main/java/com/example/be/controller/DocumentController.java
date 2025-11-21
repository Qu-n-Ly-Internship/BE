package com.example.be.controller;

import com.example.be.notification.service.NotificationPublisher;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;


    // ========================================
    // HELPER: Lấy user_id từ intern_id
    // ========================================
    private Long getUserIdByInternId(Long internId) {
        try {
            String sql = "SELECT user_id FROM intern_profiles WHERE intern_id = ?";
            return jdbcTemplate.queryForObject(sql, Long.class, internId);
        } catch (Exception e) {
            log.warn("Could not find user_id for intern_id: {}", internId);
            return null;
        }
    }

    // ========================================
    // HELPER: Gửi notification an toàn
    // ========================================
    private void sendNotificationSafe(Long userId, String type, String title, String message) {
        if (userId == null) return;
        try {
            notificationPublisher.publish(userId.toString(), type, title, message);
            log.info("✅ Notification sent to userId: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to send notification: {}", e.getMessage());
        }
    }

    // 1. Lấy tất cả tài liệu với filter
    @GetMapping("")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "q", defaultValue = "") String query
    ) {
        try {
            String sql = """
                    SELECT d.document_id, d.document_type, d.status, d.uploaded_at, d.file_detail,
                           i.intern_id, i.fullname as intern_name, i.phone,
                           u.name_uni as university_name
                    FROM intern_documents d
                    LEFT JOIN intern_profiles i ON d.intern_id = i.intern_id
                    LEFT JOIN universities u ON i.uni_id = u.uni_id
                    WHERE 1=1
                    """;

            if (status != null && !status.trim().isEmpty()) {
                sql += " AND d.status = '" + status + "'";
            }
            if (documentType != null && !documentType.trim().isEmpty()) {
                sql += " AND d.document_type = '" + documentType + "'";
            }
            if (!query.trim().isEmpty()) {
                sql += " AND (i.fullname LIKE '%" + query + "%' OR d.document_type LIKE '%" + query + "%')";
            }
            sql += " ORDER BY d.uploaded_at DESC";

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documents,
                    "total", documents.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách tài liệu: " + e.getMessage()
            ));
        }
    }

    // 10. Lấy tài liệu của chính người dùng dựa vào uploaderEmail (không cần schema change)
    @GetMapping("/my")
    public ResponseEntity<?> getMyDocuments(@RequestParam("email") String email) {
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
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", List.of(),
                        "total", 0
                ));
            }

            Long internId = ((Number) internResult.get(0).get("intern_id")).longValue();

            // Lấy TẤT CẢ documents của intern (bao gồm HR upload)
            String sql = """
                    SELECT d.document_id, d.document_name, d.document_type, d.status, d.uploaded_at, d.file_detail, d.rejection_reason
                    FROM intern_documents d
                    WHERE d.intern_id = ?
                    ORDER BY d.uploaded_at DESC
                    """;

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql, internId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documents,
                    "total", documents.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu của bạn: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy tài liệu chờ duyệt
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingDocuments() {
        try {
            String sql = """
                    SELECT d.document_id, d.document_type, d.status, d.uploaded_at, d.file_detail,
                           i.intern_id, i.fullname as intern_name, i.phone,
                           u.name_uni as university_name
                    FROM intern_documents d
                    LEFT JOIN intern_profiles i ON d.intern_id = i.intern_id
                    LEFT JOIN universities u ON i.uni_id = u.uni_id
                    WHERE d.status = 'PENDING'
                    ORDER BY d.uploaded_at ASC
                    """;

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documents,
                    "total", documents.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu chờ duyệt: " + e.getMessage()
            ));
        }
    }

    // 3. Lấy chi tiết một tài liệu
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {
        try {
            String sql = """
                    SELECT d.document_id, d.document_type, d.status, d.uploaded_at, d.file_detail,
                           i.intern_id, i.fullname as intern_name, i.phone, i.dob, i.year_of_study,
                           u.name_uni as university_name,
                           p.title as program_title
                    FROM intern_documents d
                    JOIN intern_profiles i ON d.intern_id = i.intern_id
                    LEFT JOIN universities u ON i.uni_id = u.uni_id
                    LEFT JOIN intern_programs p ON i.program_id = p.program_id
                    WHERE d.document_id = ?
                    """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.get(0)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết tài liệu: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy tài liệu theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getDocumentsByIntern(@PathVariable Long internId) {
        try {
            String sql = """
                    SELECT d.document_id, d.document_type, d.status, d.uploaded_at, d.file_detail,
                           i.intern_id, i.fullname as intern_name, i.phone,
                           u.name_uni as university_name
                    FROM intern_documents d
                    JOIN intern_profiles i ON d.intern_id = i.intern_id
                    LEFT JOIN universities u ON i.uni_id = u.uni_id
                    WHERE i.intern_id = ?
                    ORDER BY d.uploaded_at DESC
                    """;

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql, internId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documents,
                    "total", documents.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy tài liệu của thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 5. Thống kê tài liệu theo trạng thái
    @GetMapping("/stats")
    public ResponseEntity<?> getDocumentStats() {
        try {
            String sql = """
                    SELECT 
                        COUNT(*) as total,
                        SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                        SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
                        SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejected
                    FROM intern_documents
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

    // 6. Upload tài liệu mới (HỢP ĐỒNG, GIẤY TỜ - KHÔNG BAO GỒM CV)
    // CV sẽ được upload qua /api/cv/upload
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("type") String documentType,
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
                    !contentType.contains("wordprocessingml") &&
                    !contentType.contains("image"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Chỉ hỗ trợ file PDF, Word và ảnh"
                ));
            }

            // ✅ FIX: Nếu uploaderEmail được cung cấp nhưng không có internId,
            // tìm hoặc tạo intern_profile cho user này
            Long finalInternId = internId;
            if (finalInternId == null && uploaderEmail != null && !uploaderEmail.isBlank()) {
                // Tìm user theo email
                var userOpt = userRepository.findByEmail(uploaderEmail.trim());
                if (userOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Không tìm thấy user với email: " + uploaderEmail
                    ));
                }

                var user = userOpt.get();
                System.out.println("🔍 Found user: " + user.getEmail() + " - " + user.getFullName());

                // Kiểm tra xem user đã có intern_profile chưa
                String checkInternSql = "SELECT intern_id FROM intern_profiles WHERE email = ? LIMIT 1";
                try {
                    finalInternId = jdbcTemplate.queryForObject(checkInternSql, Long.class, uploaderEmail.trim());
                    System.out.println("✅ Found existing intern_profile with ID: " + finalInternId);
                } catch (Exception ex) {
                    // Chưa có intern_profile, tạo mới với các giá trị mặc định
                    System.out.println("📝 Creating new intern_profile for: " + user.getEmail());
                    String insertInternSql = """
                            INSERT INTO intern_profiles 
                            (fullname, email, uni_id, major_id, program_id, available_from, end_date, status, phone, year_of_study)
                            VALUES (?, ?, NULL, NULL, NULL, NULL, NULL, 'PENDING', '', 0)
                            """;
                    jdbcTemplate.update(insertInternSql, user.getFullName(), user.getEmail());
                    // Lấy ID vừa tạo
                    finalInternId = jdbcTemplate.queryForObject(
                            "SELECT LAST_INSERT_ID()", Long.class
                    );
                    System.out.println("✅ Created new intern_profile with ID: " + finalInternId);
                }
            }

            // Kiểm tra finalInternId không null trước khi insert
            if (finalInternId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không thể xác định intern_id. Vui lòng cung cấp internId hoặc uploaderEmail."
                ));
            }

            // TODO: Save file to storage (local/cloud)
            String fileName = file.getOriginalFilename();
            String fileDetail = String.format("%s (%.2f KB)", fileName, file.getSize() / 1024.0);
            if (uploaderEmail != null && !uploaderEmail.isBlank()) {
                fileDetail = fileDetail + " | uploadedBy=" + uploaderEmail.trim();
            }

            // Insert vào database
            String insertSql = """
                    INSERT INTO intern_documents (intern_id, document_name, document_type, uploaded_at, status, file_detail)
                    VALUES (?, ?, ?, NOW(), 'PENDING', ?)
                    """;

            jdbcTemplate.update(insertSql, finalInternId, fileName, documentType, fileDetail);



            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tải lên thành công! Chờ HR duyệt.",
                    "data", Map.of(
                            "fileName", fileName,
                            "type", documentType,
                            "size", file.getSize(),
                            "status", "PENDING"
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tải lên thất bại: " + e.getMessage()
            ));
        }
    }

    // 7. Duyệt tài liệu
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveDocument(@PathVariable Long id) {
        try {
            String checkSql = "SELECT intern_id FROM intern_documents WHERE document_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }
            Long internId = ((Number) result.get(0).get("intern_id")).longValue();
            // Update status to APPROVED
            String updateSql = """
                    UPDATE intern_documents 
                    SET status = 'APPROVED',
                        reviewed_at = NOW()
                    WHERE document_id = ?
                    """;

            jdbcTemplate.update(updateSql, id);

            Long userId = getUserIdByInternId(internId);
            sendNotificationSafe(userId, "DOCUMENT",
                    "Tài liệu đã được duyệt!",
                    "Tài liệu của bạn đã được HR phê duyệt.");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tài liệu đã được duyệt thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt tài liệu thất bại: " + e.getMessage()
            ));
        }
    }

    // 8. Từ chối tài liệu
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String rejectionReason = request.get("reason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập lý do từ chối"
                ));
            }

            String checkSql = "SELECT intern_id FROM intern_documents WHERE document_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            Long internId = ((Number) result.get(0).get("intern_id")).longValue();

            // Update status to REJECTED với lý do
            String updateSql = """
                    UPDATE intern_documents 
                    SET status = 'REJECTED',
                        rejection_reason = ?,
                        reviewed_at = NOW()
                    WHERE document_id = ?
                    """;

            jdbcTemplate.update(updateSql, rejectionReason.trim(), id);
            Long userId = getUserIdByInternId(internId);
            sendNotificationSafe(userId, "DOCUMENT",
                    "Tài liệu bị từ chối",
                    "Tài liệu của bạn bị từ chối. Lý do: " + rejectionReason.trim());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tài liệu đã bị từ chối với lý do: " + rejectionReason
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Từ chối tài liệu thất bại: " + e.getMessage()
            ));
        }
    }

    // 9. API tổng hợp cho HR - duyệt/từ chối trong 1 endpoint
    @PutMapping("/{id}/review")
    public ResponseEntity<?> reviewDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String action = request.get("action"); // "APPROVE" hoặc "REJECT"
            String note = request.get("note"); // Ghi chú (tùy chọn)

            if (action == null || (!action.equals("APPROVE") && !action.equals("REJECT"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Action phải là APPROVE hoặc REJECT"
                ));
            }

            // Check document exists
            String checkSql = "SELECT intern_id FROM intern_documents WHERE document_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, id);

            if (result.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }
            Long internId = ((Number) result.get(0).get("intern_id")).longValue();
            Long userId = getUserIdByInternId(internId);
            if (action.equals("APPROVE")) {
                // Lưu ghi chú duyệt (nếu có) vào cột rejection_reason như review_note
                String updateSql = """
                        UPDATE intern_documents 
                        SET status = 'APPROVED',
                            rejection_reason = COALESCE(?, rejection_reason),
                            reviewed_at = NOW()
                        WHERE document_id = ?
                        """;
                jdbcTemplate.update(updateSql, note != null && !note.trim().isEmpty() ? note.trim() : null, id);
                // ✅ Gửi notification
                sendNotificationSafe(userId, "DOCUMENT",
                        "Tài liệu đã được duyệt!",
                        "Tài liệu của bạn đã được HR phê duyệt.");
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Tài liệu đã được duyệt!",
                        "note", note != null ? note.trim() : ""
                ));
            } else {
                // REJECT: bắt buộc phải có lý do
                if (note == null || note.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Vui lòng nhập lý do từ chối"
                    ));
                }

                String updateSql = """
                        UPDATE intern_documents 
                        SET status = 'REJECTED',
                            rejection_reason = ?,
                            reviewed_at = NOW()
                        WHERE document_id = ?
                        """;
                jdbcTemplate.update(updateSql, note.trim(), id);
                // ✅ Gửi notification
                sendNotificationSafe(userId, "DOCUMENT",
                        "Tài liệu bị từ chối",
                        "Tài liệu của bạn bị từ chối. Lý do: " + note.trim());
                return ResponseEntity.ok(Map.of(
                        "message", "Tài liệu đã bị từ chối: " + note
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xử lý tài liệu thất bại: " + e.getMessage()
            ));
        }
    }

    // 10. Upload tài liệu cho intern cụ thể (HR upload hợp đồng)
    @PostMapping("/upload-for-intern")
    public ResponseEntity<?> uploadForIntern(
            @RequestParam("internId") Long internId,
            @RequestParam("type") String documentType,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
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

            // Kiểm tra intern tồn tại
            String checkSql = "SELECT COUNT(*) FROM intern_profiles WHERE intern_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, internId);
            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy thực tập sinh với ID: " + internId
                ));
            }

            String fileName = file.getOriginalFilename();
            String fileDetail = String.format("%s (%.2f KB) | uploadedByHR", fileName, file.getSize() / 1024.0);

            // Insert vào database với status PENDING (chờ intern xác nhận)
            String insertSql = """
                    INSERT INTO intern_documents (intern_id, document_name, document_type, uploaded_at, status, file_detail)
                    VALUES (?, ?, ?, NOW(), 'PENDING', ?)
                    """;

            jdbcTemplate.update(insertSql, internId, fileName, documentType, fileDetail);

            // ✅ THÊM: Gửi notification cho intern
            Long userId = getUserIdByInternId(internId);

            sendNotificationSafe(userId, "DOCUMENT",
                    "Bạn có hợp đồng mới cần xác nhận!",
                    "HR đã tải lên " + documentType + " cho bạn. Hãy kiểm tra và xác nhận.");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tải lên hợp đồng thành công! Chờ thực tập sinh xác nhận.",
                    "data", Map.of(
                            "fileName", fileName,
                            "type", documentType,
                            "internId", internId
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tải lên thất bại: " + e.getMessage()
            ));
        }
    }

    // 11. Xác nhận hợp đồng (Intern xác nhận đã đọc và đồng ý)
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmContract(@PathVariable Long id) {
        try {
            // Kiểm tra tài liệu tồn tại
            String checkSql = "SELECT COUNT(*) FROM intern_documents WHERE document_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            // Cập nhật status thành CONFIRMED
            String updateSql = """
                    UPDATE intern_documents 
                    SET status = 'CONFIRMED',
                        reviewed_at = NOW()
                    WHERE document_id = ?
                    """;

            jdbcTemplate.update(updateSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xác nhận hợp đồng thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xác nhận thất bại: " + e.getMessage()
            ));
        }
    }

    // 12. Xóa tài liệu
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            // Kiểm tra tài liệu tồn tại
            String checkSql = "SELECT COUNT(*) FROM intern_documents WHERE document_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            // Xóa tài liệu
            String deleteSql = "DELETE FROM intern_documents WHERE document_id = ?";
            jdbcTemplate.update(deleteSql, id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa tài liệu thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa tài liệu thất bại: " + e.getMessage()
            ));
        }
    }
}