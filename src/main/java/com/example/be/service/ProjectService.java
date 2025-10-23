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
    private final MentorContextService mentorContextService;
    private final InternProfileRepository internProfileRepository;
    private final HrContextService hrContextService;

    public ProjectService(ProjectRepository projectRepository,
                          MentorRepository mentorRepository,
                          MentorContextService mentorContextService,
                          InternProfileRepository internProfileRepository,
                          HrContextService hrContextService) {
        this.projectRepository = projectRepository;
        this.mentorRepository = mentorRepository;
        this.mentorContextService = mentorContextService;
        this.internProfileRepository = internProfileRepository;
        this.hrContextService = hrContextService;
    }

    private ProjectRequest toDto(Project project) {
        // ✅ Lấy danh sách intern thuộc project này
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

    // ✅ Lấy tất cả project của tất cả mentor
    public List<ProjectRequest> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Lấy danh sách project theo mentor (dựa trên userId)
    public List<ProjectRequest> getProjectsByUserId(Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        return projectRepository.findByMentor_Id(mentorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Tạo project mới
    public ProjectRequest createProject(ProjectRequest request, Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor với id = " + mentorId));

        Project project = toEntity(request, mentor);
        Project saved = projectRepository.save(project);
        return toDto(saved);
    }

    // ✅ Cập nhật project
    public ProjectRequest updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (!project.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không có quyền sửa project này!");
        }

        // Update fields
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setCapacity(request.getCapacity());

        Project updated = projectRepository.save(project);
        return toDto(updated);
    }

    // ✅ Xóa project
    public void deleteProject(Long id, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (!project.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không có quyền xóa project này!");
        }

        projectRepository.delete(project);
    }
    public ProjectRequest addInternToProject(Long projectId, Long internId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + projectId));

        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        Long hrId = hrContextService.getHrIdFromUserId(userId);

        boolean isMentor = (mentorId != null && project.getMentor().getId().equals(mentorId));
        boolean isHr = (hrId != null);

        if (!isMentor && !isHr) {
            throw new RuntimeException("Bạn không có quyền thêm intern vào project này!");
        }

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy intern với id = " + internId));

        // ✅ Gán project cho intern
        intern.setProgram(project);
        internProfileRepository.save(intern);

        // ✅ Lấy lại danh sách interns sau khi thêm
        List<String> internNames = internProfileRepository.findByProgram_Id(projectId)
                .stream()
                .map(InternProfile::getFullName) // hoặc getName() tùy entity của bạn
                .toList();

        // ✅ Trả về DTO chỉ chứa thông tin cần hiển thị
        return ProjectRequest.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .mentorId(project.getMentor().getId())
                .mentorName(project.getMentor().getFullName()) // hoặc getName()
                .internNames(internNames)
                .build();
    }


}
