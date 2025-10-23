package com.example.be.controller;

import com.example.be.dto.ProjectRequest;
import com.example.be.entity.InternProfile;
import com.example.be.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ✅ Lấy tất cả project của tất cả mentor
    @GetMapping
    public List<ProjectRequest> getAllProjects() {
        return projectService.getAllProjects();
    }

    // ✅ Lấy tất cả project của mentor theo userId
    @GetMapping("/mentor/{userId}")
    public List<ProjectRequest> getProjectsByMentor(@PathVariable Long userId) {
        return projectService.getProjectsByUserId(userId);
    }

    // ✅ Tạo mới project
    @PostMapping("/{userId}")
    public ProjectRequest createProject(@PathVariable Long userId, @RequestBody ProjectRequest request) {
        return projectService.createProject(request, userId);
    }

    // ✅ Cập nhật project
    @PutMapping("/{id}/{userId}")
    public ProjectRequest updateProject(@PathVariable Long id,
                                    @PathVariable Long userId,
                                    @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request, userId);
    }

    // ✅ Xóa project
    @DeleteMapping("/{id}/{userId}")
    public void deleteProject(@PathVariable Long id, @PathVariable Long userId) {
        projectService.deleteProject(id, userId);
    }

    // ✅ Thêm 1 intern vào project (cho phép cả mentor và HR)
    @PostMapping("/{projectId}/add-intern/{userId}/{internId}")
    public ProjectRequest addInternToProject(@PathVariable Long projectId,
                                             @PathVariable Long userId,
                                             @PathVariable Long internId) {
        return projectService.addInternToProject(projectId, internId, userId);
    }


}
