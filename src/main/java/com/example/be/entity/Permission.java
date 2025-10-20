package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(unique = true)
    private String name;

    private String description;
    private String module;

    @ManyToMany(mappedBy = "permissions")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new HashSet<>();

    public Set<Role> getRoles() {
        return roles != null ? roles : new HashSet<>();
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }
}
