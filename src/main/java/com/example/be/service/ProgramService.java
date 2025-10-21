package com.example.be.service;

import com.example.be.dto.ProgramRequest;
import com.example.be.entity.Hr;
import com.example.be.entity.Program;
import com.example.be.repository.HrRepository;
import com.example.be.repository.ProgramRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProgramService {

    private final ProgramRepository programRepository;
    private final HrRepository hrRepository;
    private final HrContextService hrContextService;


    public ProgramService(
            ProgramRepository programRepository,
            HrRepository hrRepository,
            HrContextService hrContextService
    ) {
        this.programRepository = programRepository;
        this.hrRepository = hrRepository;
        this.hrContextService = hrContextService;
    }

    public Program createProgram(Program program, Long userId) {
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR với id = " + hrId));

        program.setHr(hr);

        if (program.getUploadedAt() == null) {
            program.setUploadedAt(new Date());
        }

        return programRepository.save(program);
    }

    public List<ProgramRequest> getAllPrograms() {
        return programRepository.findAll().stream()
                .map(p -> new ProgramRequest(
                        p.getId(),
                        p.getProgramName(),
                        p.getDescription(),
                        p.getHr() != null ? p.getHr().getId() : null,
                        p.getHr() != null ? p.getHr().getFullname() : null,
                        p.getDateCreate(),
                        p.getDateEnd()
                ))
                .toList();
    }


    public Program getProgramById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program không tồn tại"));
    }

    public Program updateProgram(Long id, Program updatedProgram) {
        Program existing = getProgramById(id);

        // ✅ Validate dữ liệu đầu vào
        if (updatedProgram.getProgramName() == null || updatedProgram.getProgramName().trim().isEmpty()) {
            throw new RuntimeException("Tên chương trình không được để trống!");
        }

        if (updatedProgram.getDateCreate() == null) {
            throw new RuntimeException("Ngày bắt đầu không được để trống!");
        }

        if (updatedProgram.getDateEnd() == null) {
            throw new RuntimeException("Ngày kết thúc không được để trống!");
        }

        if (updatedProgram.getDateEnd().before(updatedProgram.getDateCreate())) {
            throw new RuntimeException("Ngày kết thúc phải sau  ngày bắt đầu!");
        }

        // ✅ Cập nhật dữ liệu
        existing.setProgramName(updatedProgram.getProgramName().trim());
        existing.setDateCreate(updatedProgram.getDateCreate());
        existing.setDateEnd(updatedProgram.getDateEnd());
        existing.setDescription(
                updatedProgram.getDescription() != null ? updatedProgram.getDescription().trim() : null
        );

        return programRepository.save(existing);
    }


    public void deleteProgram(Long id) {
        if (!programRepository.existsById(id)) {
            throw new RuntimeException("Program không tồn tại");
        }
        programRepository.deleteById(id);
    }
}
