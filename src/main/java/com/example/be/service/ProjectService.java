package com.example.be.service;

import com.example.be.dto.ProjectRequest;
import com.example.be.entity.InternProfile;
import com.example.be.entity.Mentors;
import com.example.be.entity.Project;
import com.example.be.repository.InternProfileRepository;
import com.example.be.repository.MentorRepository;
import com.example.be.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MentorRepository mentorRepository;
    private final InternProfileRepository internProfileRepository;
    private final HrContextService hrContextService;

    public ProjectService(ProjectRepository projectRepository,
                          MentorRepository mentorRepository,
                          InternProfileRepository internProfileRepository,
                          HrContextService hrContextService) {
        this.projectRepository = projectRepository;
        this.mentorRepository = mentorRepository;
        this.internProfileRepository = internProfileRepository;
        this.hrContextService = hrContextService;
    }

    // ✅ Chuyển từ entity -> DTO
    private ProjectRequest toDto(Project project) {
        List<String> internNames = internProfileRepository.findByProgram_Id(project.getId())
                .stream()
                .map(InternProfile::getFullName)
                .toList();

        return ProjectRequest.builder()
                .id(project.getId())
                .title(project.getTitle())
                .capacity(project.getCapacity())
                .description(project.getDescription())
                .mentorId(project.getMentor() != null ? project.getMentor().getId() : null)
                .mentorName(project.getMentor() != null && project.getMentor().getUser() != null
                        ? project.getMentor().getUser().getFullName()
                        : null)
                .internNames(internNames)
                .build();
    }

    private Project toEntity(ProjectRequest request, Mentors mentor) {
        return Project.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .mentor(mentor)
                .build();
    }

    // ✅ Lấy tất cả project (giữ nguyên)
    public List<ProjectRequest> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Lấy project theo mentor (giữ nguyên nếu cần cho hiển thị)
    public List<ProjectRequest> getProjectsByUserId(Long userId) {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ HR tạo project
    public ProjectRequest createProject(ProjectRequest request, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền tạo project!");
        }

        // Có thể chọn mentor (nếu truyền vào mentorId)
        Mentors mentor = null;
        if (request.getMentorId() != null) {
            mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor với id = " + request.getMentorId()));
        }

        Project project = toEntity(request, mentor);
        Project saved = projectRepository.save(project);
        return toDto(saved);
    }

    // ✅ HR cập nhật project
    public ProjectRequest updateProject(Long id, ProjectRequest request, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền cập nhật project!");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setCapacity(request.getCapacity());

        // Cập nhật mentor nếu có thay đổi
        if (request.getMentorId() != null) {
            Mentors mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor với id = " + request.getMentorId()));
            project.setMentor(mentor);
        }

        Project updated = projectRepository.save(project);
        return toDto(updated);
    }

    // ✅ HR xóa project
    public void deleteProject(Long id, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền xóa project!");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        projectRepository.delete(project);
    }

    // ✅ HR thêm intern vào project
    public ProjectRequest addInternToProject(Long projectId, Long internId, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền thêm intern vào project!");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + projectId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy intern với id = " + internId));

        // Gán project cho intern
        intern.setProgram(project);
        internProfileRepository.save(intern);

        List<String> internNames = internProfileRepository.findByProgram_Id(projectId)
                .stream()
                .map(InternProfile::getFullName)
                .toList();

        return ProjectRequest.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .mentorId(project.getMentor() != null ? project.getMentor().getId() : null)
                .mentorName(project.getMentor() != null ? project.getMentor().getUser().getFullName() : null)
                .internNames(internNames)
                .build();
    }
}
