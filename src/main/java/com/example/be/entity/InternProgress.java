package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "intern_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;

    @Column(name = "title")
    private String title;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}