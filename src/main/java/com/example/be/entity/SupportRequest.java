package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "intern_id")
    private Integer internId;

    // ⭐ THÊM cột user_id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "hr_id")
    private Long hrId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachment_file_id")
    private String attachmentFileId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "priority", length = 50)
    private String priority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "hr_response", columnDefinition = "TEXT")
    private String hrResponse;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (priority == null) {
            priority = "NORMAL";
        }
    }
}