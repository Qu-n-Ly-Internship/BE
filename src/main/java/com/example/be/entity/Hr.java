package com.example.be.entity;

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

    @OneToMany(mappedBy = "hr", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InternDocument> internDocuments;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id") // 🔗 liên kết đến User
    private User user;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public List<InternDocument> getInternDocuments() { return internDocuments; }
    public void setInternDocuments(List<InternDocument> internDocuments) { this.internDocuments = internDocuments; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
