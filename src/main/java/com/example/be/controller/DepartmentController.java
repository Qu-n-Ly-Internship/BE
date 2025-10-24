package com.example.be.controller;

import com.example.be.dto.DepartmentRequest;
import com.example.be.dto.MentorDepartmentRequest;
import com.example.be.entity.Department;
import com.example.be.entity.Mentors;
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

    // ========== QUẢN LÝ MENTOR TRONG DEPARTMENT ==========

    // ✅ Lấy danh sách mentor trong department
    @GetMapping("/{departmentId}/mentors")
    public List<MentorDepartmentRequest> getMentorsByDepartment(@PathVariable Long departmentId) {
        return departmentService.getMentorsByDepartment(departmentId);
    }

    // ✅ Lấy danh sách mentor chưa thuộc department nào
    @GetMapping("/mentors/available")
    public List<Mentors> getAvailableMentors() {
        return departmentService.getAvailableMentors();
    }

    // ✅ Thêm mentor vào department
    @PostMapping("/{departmentId}/mentors/{mentorId}")
    public Mentors addMentorToDepartment(
            @PathVariable Long departmentId,
            @PathVariable Long mentorId
    ) {
        return departmentService.addMentorToDepartment(departmentId, mentorId);
    }

    // ✅ Cập nhật mentor trong department (chuyển sang department khác)
    @PutMapping("/mentors/{mentorId}/department/{newDepartmentId}")
    public Mentors updateMentorDepartment(
            @PathVariable Long mentorId,
            @PathVariable Long newDepartmentId
    ) {
        return departmentService.updateMentorDepartment(mentorId, newDepartmentId);
    }

    // ✅ Xóa mentor khỏi department
    @DeleteMapping("/mentors/{mentorId}")
    public void removeMentorFromDepartment(@PathVariable Long mentorId) {
        departmentService.removeMentorFromDepartment(mentorId);
    }
}
