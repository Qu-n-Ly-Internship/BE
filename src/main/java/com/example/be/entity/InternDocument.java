package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "document_type")
    private String documentType;

    // Link hoặc chi tiết file trên Cloud
    @Column(name = "file_detail", columnDefinition = "TEXT")
    private String fileDetail;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "status")
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "reason")
    private String rejectionReason; // Lý do từ chối (nếu có)

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;



    // =======================
    // QUAN HỆ VỚI CÁC ENTITY KHÁC
    // =======================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    @JsonBackReference // tránh vòng lặp kx
    private InternProfile internProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @ManyToOne
    @JoinColumn(name = "hr_id")
    @JsonBackReference
    private Hr hr;


    // =======================
    // TIỆN ÍCH
    // =======================

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
