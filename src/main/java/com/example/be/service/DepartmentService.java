package com.example.be.service;

import com.example.be.entity.Department;
import com.example.be.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getByProgram(Long programId) {
        return departmentRepository.findByProgramId(programId);
    }

    public Department createOne(Long programId, Department department) {
        department.setProgramId(programId);
        return departmentRepository.save(department);
    }

    public List<Department> createMany(Long programId, List<Department> departments) {
        for (Department d : departments) {
            d.setProgramId(programId);
        }
        return departmentRepository.saveAll(departments);
    }

    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }
}
