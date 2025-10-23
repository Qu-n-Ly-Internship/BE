package com.example.be.service;

import com.example.be.dto.DepartmentRequest;
import com.example.be.entity.Department;
import com.example.be.entity.Hr;
import com.example.be.entity.Program;
import com.example.be.repository.DepartmentRepository;
import com.example.be.repository.HrRepository;
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
    private final HrContextService hrContextService;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            HrRepository hrRepository,
            HrContextService hrContextService
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.hrRepository = hrRepository;
        this.hrContextService = hrContextService;
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

        Program program = programRepository.findById(programId)
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

        Program program = programRepository.findById(programId)
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
}
