package com.example.be.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DepartmentRequest
{
    // --- Getters & Setters ---
    private Long id;
    private String nameDepartment;
    private Integer capacity;
    private Long programId;
    private String hrName;

    // Constructor
    public DepartmentRequest(Long id, String nameDepartment, Integer capacity, Long programId, String hrName) {
        this.id = id;
        this.nameDepartment = nameDepartment;
        this.capacity = capacity;
        this.programId = programId;
        this.hrName = hrName;
    }

    // --- toString() (hữu ích khi debug/log) ---
    @Override
    public String toString() {
        return "DepartmentDTO{" +
                "id=" + id +
                ", nameDepartment='" + nameDepartment + '\'' +
                ", capacity=" + capacity +
                ", programId=" + programId +
                ", hrName='" + hrName + '\'' +
                '}';
    }
}
