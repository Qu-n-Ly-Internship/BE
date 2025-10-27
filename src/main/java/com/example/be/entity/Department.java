package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(name = "name_department", nullable = false)
    private String nameDepartment;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "program_id")
    private Long programId;

    // ✅ Người tạo (HR)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_hr")
    @JsonBackReference
    private Hr hr;

    // ✅ Danh sách mentor trong department
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Mentors> mentors;

    public Hr getHr() {
        return hr;
    }

    public void setHr(Hr hr) {
        this.hr = hr;
    }

    public List<Mentors> getMentors() {
        return mentors;
    }

    public void setMentors(List<Mentors> mentors) {
        this.mentors = mentors;
    }


    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNameDepartment() { return nameDepartment; }
    public void setNameDepartment(String nameDepartment) { this.nameDepartment = nameDepartment; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }
}
