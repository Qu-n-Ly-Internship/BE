package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "filename")
    private String filename;

    @Column(name = "uploaded_by")
    private Integer uploadedBy; // user_id của người upload

    @Column(name = "status")
    private String status; // PENDING, APPROVED, REJECTED

    // =======================
    // QUAN HỆ VỚI CÁC ENTITY KHÁC
    // =======================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id")
    @JsonBackReference
    private InternProfile internProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "hr_id")
    @JsonBackReference
    private Hr hr;

    // =======================
    // TIỆN ÍCH
    // =======================

    @PrePersist
    protected void onCreate() {
        // uploaded_by sẽ được set từ controller
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
