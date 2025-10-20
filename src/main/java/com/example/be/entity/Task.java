package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "assigned_to") // intern_id
    private InternProfile assignedTo;

    @ManyToOne
    @JoinColumn(name = "assigned_by") // user_id
    private User assignedBy;

    private String priority; // LOW, MEDIUM, HIGH

    private String status; // NEW, IN_PROGRESS, COMPLETED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String description;
}
