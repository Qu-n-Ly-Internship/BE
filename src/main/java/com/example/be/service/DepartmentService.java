package com.example.be.service;

import com.example.be.dto.DepartmentRequest;
import com.example.be.dto.MentorDepartmentDTO;
import com.example.be.dto.MentorDepartmentRequest;
import com.example.be.entity.Department;
import com.example.be.entity.Hr;
import com.example.be.entity.Mentors;
import com.example.be.repository.DepartmentRepository;
import com.example.be.repository.HrRepository;
import com.example.be.repository.MentorRepository;
import com.example.be.repository.ProgramRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final HrRepository hrRepository;
    private final MentorRepository mentorRepository;
    private final HrContextService hrContextService;
    private final MentorContextService mentorContextService;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            HrRepository hrRepository,
            MentorRepository mentorRepository,
            HrContextService hrContextService,
            MentorContextService mentorContextService
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.hrRepository = hrRepository;
        this.mentorRepository = mentorRepository;
        this.hrContextService = hrContextService;
        this.mentorContextService = mentorContextService;
    }


    public List<DepartmentRequest> getByProgram(Long programId) {
        List<Department> departments = departmentRepository.findByProgramId(programId);

        return departments.stream()
                .map(d -> new DepartmentRequest(
                        d.getId(),
                        d.getNameDepartment(),
                        d.getCapacity(),
                        d.getProgramId(),
                        d.getHr() != null ? d.getHr().getFullname() : null
                ))
                .collect(Collectors.toList());
    }


    // ✅ Tạo 1 department gắn với programId và userId
    public Department createOne(Long programId, Department department, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR với id = " + hrId));

        programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program không tồn tại với id = " + programId));

        // Validate tên phòng ban
        if (department.getNameDepartment() == null || department.getNameDepartment().trim().isEmpty()) {
            throw new RuntimeException("Tên phòng ban không được để trống!");
        }

        department.setProgramId(programId);
        department.setHr(hr);

        return departmentRepository.save(department);
    }

    // ✅ Tạo nhiều department cho cùng 1 program
    public List<Department> createMany(Long programId, List<Department> departments, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR với id = " + hrId));

        programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program không tồn tại với id = " + programId));

        for (Department d : departments) {
            if (d.getNameDepartment() == null || d.getNameDepartment().trim().isEmpty()) {
                throw new RuntimeException("Tên phòng ban không được để trống!");
            }
            d.setProgramId(programId);
            d.setHr(hr);
        }

        return departmentRepository.saveAll(departments);
    }

    // ✅ Cập nhật thông tin department
    public Department updateDepartment(Long id, Department updated) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department không tồn tại"));

        if (updated.getNameDepartment() == null || updated.getNameDepartment().trim().isEmpty()) {
            throw new RuntimeException("Tên phòng ban không được để trống!");
        }

        existing.setNameDepartment(updated.getNameDepartment().trim());

        existing.setCapacity(updated.getCapacity());

        return departmentRepository.save(existing);
    }

    // ✅ Xóa department theo id
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("Department không tồn tại");
        }
        departmentRepository.deleteById(id);
    }

    // ========== QUẢN LÝ MENTOR TRONG DEPARTMENT ==========
    // ✅ Lấy danh sách mentor trong department
    public List<MentorDepartmentRequest> getMentorsByDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department không tồn tại với id = " + departmentId));

        List<Mentors> mentors = mentorRepository.findByDepartment(department);

        return mentors.stream()
                .map(m -> new MentorDepartmentRequest(
                        m.getId(),
                        m.getDepartment() != null ? m.getDepartment().getId() : null,
                        m.getFullName(),
                        department.getNameDepartment()
                ))
                .collect(Collectors.toList());
    }

    public MentorDepartmentDTO addMentorToDepartment(Long departmentId, Long mentorId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department không tồn tại với id = " + departmentId));

        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor không tồn tại với id = " + mentorId));

        if (mentor.getDepartment() != null) {
            throw new RuntimeException("Mentor đã thuộc department khác");
        }

        mentor.setDepartment(department);
        Mentors saved = mentorRepository.save(mentor);

        // ✅ Trả về DTO — không còn vòng lặp entity
        return new MentorDepartmentDTO(
                saved.getId(),
                saved.getFullName(),
                saved.getUser() != null ? saved.getUser().getEmail() : null,
                department.getId(),
                department.getNameDepartment()
        );
    }


    // ✅ Cập nhật mentor sang department khác (trả DTO thay vì entity)
    public MentorDepartmentDTO updateMentorDepartment(Long mentorId, Long newDepartmentId) {
        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor với id = " + mentorId));

        Department newDepartment = departmentRepository.findById(newDepartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy department với id = " + newDepartmentId));

        mentor.setDepartment(newDepartment);
        Mentors updatedMentor = mentorRepository.save(mentor);

        // 🔁 Trả về DTO để frontend dùng
        return new MentorDepartmentDTO(
                updatedMentor.getId(),
                updatedMentor.getFullName(),
                updatedMentor.getUser() != null ? updatedMentor.getUser().getEmail() : null,
                newDepartment.getId(),
                newDepartment.getNameDepartment()
        );
    }


    // ✅ Xóa mentor khỏi department (dựa theo mentorId)
    public void removeMentorFromDepartment(Long mentorId) {
        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor không tồn tại với id = " + mentorId));

        mentor.setDepartment(null);
        mentorRepository.save(mentor);
    }


    // ✅ Lấy danh sách mentor chưa thuộc department nào
    public List<Mentors> getAvailableMentors() {
        return mentorRepository.findByDepartmentIsNull();
    }
}
