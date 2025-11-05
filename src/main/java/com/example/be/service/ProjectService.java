package com.example.be.service;

import com.example.be.dto.InternRequest;
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
    private final MentorContextService mentorContextService;

    public ProjectService(ProjectRepository projectRepository,
                          MentorRepository mentorRepository,
                          InternProfileRepository internProfileRepository,
                          HrContextService hrContextService,
                          MentorContextService mentorContextService) {
        this.projectRepository = projectRepository;
        this.mentorRepository = mentorRepository;
        this.internProfileRepository = internProfileRepository;
        this.hrContextService = hrContextService;
        this.mentorContextService = mentorContextService;
    }

    // ✅ Chuyển từ entity -> DTO
    private ProjectRequest toDto(Project project) {
        // Lấy danh sách intern của project
        List<InternRequest> interns = internProfileRepository.findByProgram_Id(project.getId())
                .stream()
                .map(intern -> InternRequest.builder()
                        .id(intern.getId())
                        .fullName(intern.getFullName())
                        .build())
                .collect(Collectors.toList());

        return ProjectRequest.builder()
                .id(project.getId())
                .title(project.getTitle())
                .capacity(project.getCapacity())
                .description(project.getDescription())
                .mentorId(project.getMentor() != null ? project.getMentor().getId() : null)
                .mentorName(project.getMentor() != null && project.getMentor().getUser() != null
                        ? project.getMentor().getUser().getFullName()
                        : null)
                .internNames(interns)
                .build();
    }

    // ✅ Chuyển DTO -> entity
    private Project toEntity(ProjectRequest request, Mentors mentor) {
        return Project.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .mentor(mentor)
                .build();
    }

    // ✅ Lấy tất cả project
    public List<ProjectRequest> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ✅ Lấy project theo userId (chỉ lấy project của mentor đó)
    public List<ProjectRequest> getProjectsByUserId(Long userId) {
        // Lấy mentorId từ userId qua MentorContextService
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (mentorId == null) {
            throw new RuntimeException("Không tìm thấy mentor tương ứng với userId = " + userId);
        }

        // Lọc project theo mentorId
        return projectRepository.findAll().stream()
                .filter(p -> p.getMentor() != null && p.getMentor().getId().equals(mentorId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    // ✅ Mentor tạo project
    public ProjectRequest createProject(ProjectRequest request, Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (mentorId == null) {
            throw new RuntimeException("Bạn không có quyền tạo project!");
        }

        // ✅ Gán mentor đang đăng nhập cho project
        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor với id = " + mentorId));

        Project project = toEntity(request, mentor);
        Project saved = projectRepository.save(project);

        return toDto(saved);
    }

    // ✅ Mentor cập nhật project của chính mình
    public ProjectRequest updateProject(Long id, ProjectRequest request, Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (mentorId == null) {
            throw new RuntimeException("Bạn không có quyền cập nhật project!");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        // ✅ Chỉ mentor sở hữu project mới được chỉnh sửa
        if (!project.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không thể chỉnh sửa project của mentor khác!");
        }

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setCapacity(request.getCapacity());

        Project updated = projectRepository.save(project);
        return toDto(updated);
    }

    // ✅ Mentor xóa project của chính mình
    public void deleteProject(Long id, Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (mentorId == null) {
            throw new RuntimeException("Bạn không có quyền xóa project!");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project với id = " + id));

        // ✅ Chỉ mentor sở hữu project mới được xóa
        if (!project.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không thể xóa project của mentor khác!");
        }

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

        List<InternRequest> interns = internProfileRepository.findByProgram_Id(projectId)
                .stream()
                .map(i -> InternRequest.builder()
                        .id(i.getId())
                        .fullName(i.getFullName())
                        .build())
                .collect(Collectors.toList());

        return ProjectRequest.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .mentorId(project.getMentor() != null ? project.getMentor().getId() : null)
                .mentorName(project.getMentor() != null ? project.getMentor().getUser().getFullName() : null)
                .internNames(interns)
                .build();
    }


    // ✅ HR chuyển intern sang project khác
    public InternProfile transferInternToAnotherProject(Long internId, Long newProjectId, Long userId) {
        // 1️⃣ Xác thực HR
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền chuyển intern giữa các project!");
        }

        // 2️⃣ Tìm intern
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh có ID: " + internId));

        // 3️⃣ Tìm project mới
        Project newProject = projectRepository.findById(newProjectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy project có ID: " + newProjectId));

        // 4️⃣ Kiểm tra trùng lặp
        if (intern.getProgram() != null && intern.getProgram().getId().equals(newProjectId)) {
            throw new RuntimeException("Intern đã thuộc project này rồi.");
        }

        // 5️⃣ Cập nhật project mới
        intern.setProgram(newProject);
        return internProfileRepository.save(intern);
    }

    // ✅ HR xóa intern khỏi project
    public InternProfile removeInternFromProject(Long internId, Long userId) {
        // 1️⃣ Xác thực HR
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Bạn không có quyền xóa intern khỏi project!");
        }

        // 2️⃣ Tìm intern
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh có ID: " + internId));

        // 3️⃣ Kiểm tra xem intern có đang thuộc project không
        if (intern.getProgram() == null) {
            throw new RuntimeException("Intern này hiện không thuộc bất kỳ project nào.");
        }

        // 4️⃣ Gỡ liên kết với project
        intern.setProgram(null);
        return internProfileRepository.save(intern);
    }


    public List<ProjectRequest> filterProjects(Long programId, Long departmentId) {
        List<Project> projects;

        if (programId != null && departmentId == null) {
            // ✅ Lọc theo Program (gộp nhiều department)
            projects = projectRepository.findByMentorDepartmentProgramId(programId);
        } else if (programId != null && departmentId != null) {
            // ✅ Lọc theo Department (cần biết program cha)
            projects = projectRepository.findByMentorDepartmentId(departmentId);
        } else {
            throw new IllegalArgumentException("Bạn phải chọn ít nhất một Program trước khi lọc project.");
        }

        return projects.stream().map(this::toDto).collect(Collectors.toList());
    }


}
