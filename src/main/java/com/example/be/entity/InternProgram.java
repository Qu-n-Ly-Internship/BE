package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "intern_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long id;

    private String title;

    private Integer capacity;

    @Column(columnDefinition = "TEXT")
    private String description;


    @ManyToOne
    @JoinColumn(name = "mentor_id")
    private User mentor;
}
