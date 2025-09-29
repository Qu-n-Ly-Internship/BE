package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "intern_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "intern_id")
    private InternProfile internProfile;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "file_detail")
    private String fileDetail;

    // Thêm trường để lưu lý do từ chối
    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Thêm trường lưu người duyệt
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}