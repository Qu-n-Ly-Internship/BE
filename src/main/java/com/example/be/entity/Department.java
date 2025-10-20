package com.example.be.entity;

import jakarta.persistence.*;

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
    private Long programId; // chỉ lưu FK, không ánh xạ object

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
