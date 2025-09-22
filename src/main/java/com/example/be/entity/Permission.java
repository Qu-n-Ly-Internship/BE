package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // READ_USERS, CREATE_USERS, UPDATE_USERS, DELETE_USERS, etc.

    private String description;
    private String module; // USER_MANAGEMENT, INTERNSHIP_MANAGEMENT, etc.
}