package com.example.be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter

@Entity
@Table(name = "intern_reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne
    @JoinColumn(name = "hr_id", nullable = false)
    private Hr hr;

    private BigDecimal overallScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    private LocalDateTime createdAt;
}
