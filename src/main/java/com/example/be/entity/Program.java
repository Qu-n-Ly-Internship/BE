package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long id;

    @Column(name = "program_name", nullable = false)
    private String programName;


    @Column(name = "date_create")
    private Date dateCreate;


    @Column(name = "date_end")
    private Date dateEnd;

    @Column(name = "description")
    private String description;

    // ✅ Người tạo (HR)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_id")
    @JsonBackReference  // Ngăn vòng lặp khi serialize JSON
    private Hr hr;

    // ✅ Thời điểm tạo (tự động set)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Date uploadedAt;

    @PrePersist
    protected void onCreate() {
        java.time.ZonedDateTime nowInHCM = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        this.uploadedAt = java.util.Date.from(nowInHCM.toInstant());
    }


    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public Date getDateCreate() { return dateCreate; }
    public void setDateCreate(Date dateCreate) { this.dateCreate = dateCreate; }

    public Date getDateEnd() { return dateEnd; }
    public void setDateEnd(Date dateEnd) { this.dateEnd = dateEnd; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Hr getHr() { return hr; }
    public void setHr(Hr hr) { this.hr = hr; }

    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
}
