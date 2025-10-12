package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CV {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "intern_id")
    private Integer internId;  // Nullable, set sau khi approve

    @Column(name = "user_id", nullable = false)
    private Integer userId;  // User gửi CV (sinh viên)

    @Column(name = "file_type")
    private String fileType;  // CV, Resume, etc.

    @Column(name = "storage_path")
    private String storagePath;  // Đường dẫn lưu file

    @Column(name = "filename")
    private String filename;  // Tên file gốc

    @Column(name = "mime_type")
    private String mimeType;  // application/pdf, etc.

    @Column(name = "size")
    private Integer size;  // Kích thước file (bytes)

    @Column(name = "uploaded_by")
    private Integer uploadedBy;  // FK tới users - người upload

    @Column(name = "status")
    private String status;  // PENDING, APPROVED, REJECTED

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "email_sended")
    private Boolean emailSended;  // Đã gửi email chưa

    @Column(name = "rejection_reason")
    private String rejectionReason;  // Lý do từ chối (nếu REJECTED)

    @Column(name = "reviewed_by")
    private Integer reviewedBy;  // FK tới users - HR duyệt

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;  // Thời gian duyệt

    // Helper method để lấy thông tin user
    @Transient
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    @Transient
    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    @Transient
    public boolean isRejected() {
        return "REJECTED".equals(this.status);
    }
}