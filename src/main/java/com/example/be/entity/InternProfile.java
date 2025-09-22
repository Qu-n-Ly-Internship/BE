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
    private Long internId;

    @Column(name = "uni_id")
    private Long uniId;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(name = "phone")
    private String phone;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "hr_id")
    private Long hrId;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uni_id", insertable = false, updatable = false)
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", insertable = false, updatable = false)
    private Major major;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_id", insertable = false, updatable = false)
    private HR hr;
}