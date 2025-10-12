package com.example.be.controller;

import com.example.be.config.JwtUtil;
import com.example.be.entity.CV;
import com.example.be.entity.User;
import com.example.be.repository.CVRepository;
import com.example.be.repository.UserRepository;
import com.example.be.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CVController {
    private static final Logger logger = LoggerFactory.getLogger(CVController.class);

    private final CVRepository cvRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    // Thư mục lưu CV (có thể config trong application.properties)
    private static final String UPLOAD_DIR = "uploads/cvs/";

    // ==================== USER ENDPOINTS ====================

    /**
     * User upload CV
     * Mỗi user chỉ được upload 1 CV
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            // Lấy user hiện tại từ token
            String email = extractEmailFromToken(bearerToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

            // Kiểm tra xem user đã upload CV chưa
            List<CV> existingCVs = cvRepository.findAll().stream()
                    .filter(cv -> cv.getUserId().equals(user.getId().intValue()))
                    .collect(Collectors.toList());

            if (!existingCVs.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Bạn đã upload CV rồi! Mỗi user chỉ được upload 1 CV."
                ));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File không được để trống!"
                ));
            }

            // Kiểm tra kích thước (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File không được vượt quá 5MB!"
                ));
            }

            // Kiểm tra định dạng file
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Chỉ chấp nhận file PDF, DOC, DOCX!"
                ));
            }

            // Lưu file vào server
            String filename = saveFile(file, user.getId());

            // Tạo record CV trong database
            CV cv = CV.builder()
                    .userId(user.getId().intValue())
                    .uploadedBy(user.getId().intValue())
                    .filename(file.getOriginalFilename())
                    .storagePath(filename)
                    .fileType("CV")
                    .mimeType(contentType)
                    .size((int) file.getSize())
                    .status("PENDING")
                    .uploadedAt(LocalDateTime.now())
                    .emailSended(false)
                    .build();

            CV savedCV = cvRepository.save(cv);

            logger.info("✅ User {} uploaded CV successfully. CV ID: {}", email, savedCV.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload CV thành công! Đang chờ HR duyệt.",
                    "data", Map.of(
                            "cvId", savedCV.getId(),
                            "filename", savedCV.getFilename(),
                            "status", savedCV.getStatus(),
                            "uploadedAt", savedCV.getUploadedAt()
                    )
            ));

        } catch (Exception e) {
            logger.error("❌ Upload CV failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Upload thất bại: " + e.getMessage()
            ));
        }
    }

    /**
     * User xem CV của mình
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyCV(@RequestHeader("Authorization") String bearerToken) {
        try {
            String email = extractEmailFromToken(bearerToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

            List<CV> cvs = cvRepository.findAll().stream()
                    .filter(cv -> cv.getUserId().equals(user.getId().intValue()))
                    .collect(Collectors.toList());

            if (cvs.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Bạn chưa upload CV",
                        "data", null
                ));
            }

            CV cv = cvs.get(0);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", formatCVResponse(cv)
            ));

        } catch (Exception e) {
            logger.error("❌ Get my CV failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // ==================== HR ENDPOINTS ====================

    /**
     * HR xem tất cả CV đang chờ duyệt
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCVs() {
        try {
            List<CV> pendingCVs = cvRepository.findAll().stream()
                    .filter(cv -> "PENDING".equals(cv.getStatus()))
                    .sorted(Comparator.comparing(CV::getUploadedAt))
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = pendingCVs.stream()
                    .map(this::formatCVResponseWithUser)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result,
                    "total", result.size()
            ));

        } catch (Exception e) {
            logger.error("❌ Get pending CVs failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * HR xem tất cả CV (có filter)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllCVs(
            @RequestParam(value = "status", required = false) String status
    ) {
        try {
            List<CV> allCVs = cvRepository.findAll();

            // Filter theo status nếu có
            if (status != null && !status.trim().isEmpty()) {
                allCVs = allCVs.stream()
                        .filter(cv -> status.equalsIgnoreCase(cv.getStatus()))
                        .collect(Collectors.toList());
            }

            // Sort theo thời gian
            allCVs.sort(Comparator.comparing(CV::getUploadedAt).reversed());

            List<Map<String, Object>> result = allCVs.stream()
                    .map(this::formatCVResponseWithUser)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result,
                    "total", result.size()
            ));

        } catch (Exception e) {
            logger.error("❌ Get all CVs failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * HR duyệt CV và gửi email
     */
    @PostMapping("/{cvId}/approve")
    @Transactional
    public ResponseEntity<?> approveCV(
            @PathVariable Long cvId,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            // Lấy HR hiện tại
            String hrEmail = extractEmailFromToken(bearerToken);
            User hr = userRepository.findByEmail(hrEmail)
                    .orElseThrow(() -> new RuntimeException("HR không tồn tại!"));

            // Lấy CV
            CV cv = cvRepository.findById(cvId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy CV!"));

            if (!"PENDING".equals(cv.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV này đã được xử lý rồi!"
                ));
            }

            // Cập nhật status
            cv.setStatus("APPROVED");
            cv.setReviewedBy(hr.getId().intValue());
            cv.setReviewedAt(LocalDateTime.now());
            cvRepository.save(cv);

            // Lấy thông tin user để gửi email
            User user = userRepository.findById(cv.getUserId().longValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            // Gửi email
            try {
                emailService.sendStatusEmail(
                        user.getEmail(),
                        user.getFullName(),
                        "accepted",  // tag trong email template
                        null  // không cần lý do khi approve
                );

                cv.setEmailSended(true);
                cvRepository.save(cv);

                logger.info("✅ Sent approval email to {}", user.getEmail());
            } catch (Exception emailEx) {
                logger.error("❌ Failed to send email", emailEx);
                // Không rollback transaction, chỉ log lỗi
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã duyệt CV và gửi email thông báo!",
                    "data", formatCVResponse(cv)
            ));

        } catch (Exception e) {
            logger.error("❌ Approve CV failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Duyệt CV thất bại: " + e.getMessage()
            ));
        }
    }

    /**
     * HR từ chối CV và gửi email
     */
    @PostMapping("/{cvId}/reject")
    @Transactional
    public ResponseEntity<?> rejectCV(
            @PathVariable Long cvId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập lý do từ chối!"
                ));
            }

            // Lấy HR hiện tại
            String hrEmail = extractEmailFromToken(bearerToken);
            User hr = userRepository.findByEmail(hrEmail)
                    .orElseThrow(() -> new RuntimeException("HR không tồn tại!"));

            // Lấy CV
            CV cv = cvRepository.findById(cvId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy CV!"));

            if (!"PENDING".equals(cv.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "CV này đã được xử lý rồi!"
                ));
            }

            // Cập nhật status
            cv.setStatus("REJECTED");
            cv.setRejectionReason(reason.trim());
            cv.setReviewedBy(hr.getId().intValue());
            cv.setReviewedAt(LocalDateTime.now());
            cvRepository.save(cv);

            // Lấy thông tin user để gửi email
            User user = userRepository.findById(cv.getUserId().longValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            // Gửi email
            try {
                emailService.sendStatusEmail(
                        user.getEmail(),
                        user.getFullName(),
                        "rejected",  // tag trong email template
                        reason.trim()
                );

                cv.setEmailSended(true);
                cvRepository.save(cv);

                logger.info("✅ Sent rejection email to {}", user.getEmail());
            } catch (Exception emailEx) {
                logger.error("❌ Failed to send email", emailEx);
                // Không rollback transaction, chỉ log lỗi
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã từ chối CV và gửi email thông báo!",
                    "data", formatCVResponse(cv)
            ));

        } catch (Exception e) {
            logger.error("❌ Reject CV failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Từ chối CV thất bại: " + e.getMessage()
            ));
        }
    }

    // ==================== HELPER METHODS ====================

    private String extractEmailFromToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return jwtUtil.extractEmail(token);
        }
        throw new RuntimeException("Invalid token format");
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/msword") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }

    private String saveFile(MultipartFile file, Long userId) throws IOException {
        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique: userId_timestamp_originalFilename
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFilename = file.getOriginalFilename();
        String filename = userId + "_" + timestamp + "_" + originalFilename;

        // Lưu file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return filename;
    }

    private Map<String, Object> formatCVResponse(CV cv) {
        Map<String, Object> response = new HashMap<>();
        response.put("cvId", cv.getId());
        response.put("filename", cv.getFilename());
        response.put("fileType", cv.getFileType());
        response.put("size", cv.getSize());
        response.put("status", cv.getStatus());
        response.put("uploadedAt", cv.getUploadedAt());
        response.put("emailSended", cv.getEmailSended());

        if (cv.getReviewedAt() != null) {
            response.put("reviewedAt", cv.getReviewedAt());
        }
        if (cv.getRejectionReason() != null) {
            response.put("rejectionReason", cv.getRejectionReason());
        }

        return response;
    }

    private Map<String, Object> formatCVResponseWithUser(CV cv) {
        Map<String, Object> response = formatCVResponse(cv);

        // Thêm thông tin user
        userRepository.findById(cv.getUserId().longValue()).ifPresent(user -> {
            response.put("userEmail", user.getEmail());
            response.put("userFullName", user.getFullName());
            response.put("userId", user.getId());
        });

        return response;
    }
}