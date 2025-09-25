package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    private String status;

    // Quyền riêng cho user này
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserPermission> userPermissions;
}