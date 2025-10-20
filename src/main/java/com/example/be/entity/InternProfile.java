package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "intern_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intern_id")
    private Long id;

    @Column(name = "fullname")
    private String fullName;

    @Column(name = "dob")
    private LocalDate dateOfBirth;

    @Column(name = "major_id")
    private Integer majorId;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private InternProgram program;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    private String phone;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @ManyToOne
    @JoinColumn(name = "uni_id")
    private University university;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @Column(name = "email")
    private String email;

}