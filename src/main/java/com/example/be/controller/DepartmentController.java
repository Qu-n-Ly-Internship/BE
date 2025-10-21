package com.example.be.controller;

import com.example.be.entity.Department;
import com.example.be.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    // Lấy tất cả department theo program
    @GetMapping("/program/{programId}")
    public List<Department> getByProgram(@PathVariable Long programId) {
        return departmentService.getByProgram(programId);
    }

    // ✅ Tạo 1 department cho program
    @PostMapping("/program/{programId}")
    public Department createOne(
            @PathVariable Long programId,
            @RequestBody Department department
    ) {
        return departmentService.createOne(programId, department);
    }


    @PostMapping("/program/{programId}/batch")
    public List<Department> createMany(
            @PathVariable Long programId,
            @RequestBody List<Department> departments
    ) {
        return departmentService.createMany(programId, departments);
    }

    // Xóa department
    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
