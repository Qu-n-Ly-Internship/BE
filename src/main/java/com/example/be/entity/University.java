package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "universities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class University {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uni_id")
    private Long id;

    @Column(name = "name_uni")
    private String name;
}