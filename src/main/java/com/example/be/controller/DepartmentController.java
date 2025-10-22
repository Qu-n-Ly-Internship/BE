package com.example.be.controller;

import com.example.be.dto.DepartmentRequest;
import com.example.be.entity.Department;
import com.example.be.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // ✅ Lấy danh sách department theo programId
    @GetMapping("/program/{programId}")
    public List<DepartmentRequest> getByProgram(@PathVariable Long programId) {
        return departmentService.getByProgram(programId);
    }


    // ✅ Tạo 1 department cho 1 program (có userId)
    @PostMapping("/program/{programId}/user/{userId}")
    public Department createOne(
            @PathVariable Long programId,
            @PathVariable Long userId,
            @RequestBody Department department
    ) {
        return departmentService.createOne(programId, department, userId);
    }

    // ✅ Tạo nhiều department cùng lúc
    @PostMapping("/program/{programId}/user/{userId}/batch")
    public List<Department> createMany(
            @PathVariable Long programId,
            @PathVariable Long userId,
            @RequestBody List<Department> departments
    ) {
        return departmentService.createMany(programId, departments, userId);
    }

    // ✅ Cập nhật department
    @PutMapping("/{id}")
    public Department updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department
    ) {
        return departmentService.updateDepartment(id, department);
    }

    // ✅ Xóa department theo id
    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
