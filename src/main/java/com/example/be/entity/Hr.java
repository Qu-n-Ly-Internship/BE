package com.example.be.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

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

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public List<InternDocument> getInternDocuments() { return internDocuments; }
    public void setInternDocuments(List<InternDocument> internDocuments) { this.internDocuments = internDocuments; }


    public interface HrRepository extends JpaRepository<Hr, Long> {
    }
}
