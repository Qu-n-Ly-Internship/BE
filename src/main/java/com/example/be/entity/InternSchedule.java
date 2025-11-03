package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "intern_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "intern_id")
    private InternProfile intern;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private Project program;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "title", nullable = true)
    private String title; // Tiêu đề lịch

    private String description; // Mô tả chi tiết

    @Column(name = "start_time")
    private String startTime; // Thời gian bắt đầu (format: "HH:mm")

    @Column(name = "end_time")
    private String endTime; // Thời gian kết thúc (format: "HH:mm")

    private String location; // Địa điểm

    @Column(name = "task_type")
    private String taskType; // Loại công việc: TRAINING, MEETING, TASK, etc.

    private LocalDate date;

    private String status; // PLANNED / COMPLETED / CANCELLED

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}