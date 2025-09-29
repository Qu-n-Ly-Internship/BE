package com.example.be.controller;

import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

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

            String sql = """
                SELECT d.document_id, d.document_type, d.status, d.uploaded_at, d.file_detail, d.rejection_reason
                FROM intern_documents d
                WHERE d.file_detail LIKE ?
                ORDER BY d.uploaded_at DESC
                """;

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(sql, "%uploadedBy=" + email + "%");

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

    // 6. Upload tài liệu mới
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

            // TODO: Save file to storage (local/cloud)
            String fileName = file.getOriginalFilename();
            String fileDetail = String.format("%s (%.2f KB)", fileName, file.getSize() / 1024.0);
            if (uploaderEmail != null && !uploaderEmail.isBlank()) {
                fileDetail = fileDetail + " | uploadedBy=" + uploaderEmail.trim();
            }

            // Insert vào database
            String insertSql = """
                INSERT INTO intern_documents (intern_id, document_type, uploaded_at, status, file_detail)
                VALUES (?, ?, NOW(), 'PENDING', ?)
                """;

            jdbcTemplate.update(insertSql, internId, documentType, fileDetail);

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
            // Check document exists
            String checkSql = "SELECT COUNT(*) FROM intern_documents WHERE document_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            // Update status to APPROVED
            String updateSql = """
                UPDATE intern_documents 
                SET status = 'APPROVED',
                    reviewed_at = NOW()
                WHERE document_id = ?
                """;

            jdbcTemplate.update(updateSql, id);

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


            String checkSql = "SELECT COUNT(*) FROM intern_documents WHERE document_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

            // Update status to REJECTED với lý do
            String updateSql = """
                UPDATE intern_documents 
                SET status = 'REJECTED',
                    rejection_reason = ?,
                    reviewed_at = NOW()
                WHERE document_id = ?
                """;

            jdbcTemplate.update(updateSql, rejectionReason.trim(), id);

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
            String checkSql = "SELECT COUNT(*) FROM intern_documents WHERE document_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy tài liệu với ID: " + id
                ));
            }

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

                return ResponseEntity.ok(Map.of(
                        "success", true,
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
}