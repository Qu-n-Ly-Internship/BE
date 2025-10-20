package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "hr")
public class Hr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hr_id")
    private Long id;

    @Column(name = "fullname", nullable = false)
    private String fullname;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    // ✅ Liên kết 1-n với Program (được serialize ra JSON)
    @OneToMany(mappedBy = "hr", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Program> programs;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Program> getPrograms() { return programs; }
    public void setPrograms(List<Program> programs) { this.programs = programs; }
}
