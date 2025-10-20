package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "mentors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mentors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentor_id")
    private Long id;

    @Column(name = "fullname", nullable = false)
    private String fullName;

    @Column(name = "department_id")
    private Long departmentId; // có thể null, đúng với cột bạn có trong DB

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;
}
