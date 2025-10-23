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

    private LocalDate date;

    private String status; // PLANNED / COMPLETED / CANCELLED

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
