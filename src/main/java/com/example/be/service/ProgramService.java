package com.example.be.service;

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

    public ProgramService(ProgramRepository programRepository, HrRepository hrRepository) {
        this.programRepository = programRepository;
        this.hrRepository = hrRepository;
    }

    // ✅ HR tạo program mới → tự động lưu thời gian tạo (uploadedAt)
    public Program createProgram(Program program, Long hrId) {
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR với id = " + hrId));

        program.setHr(hr);

        if (program.getUploadedAt() == null) {
            program.setUploadedAt(new Date());
        }

        return programRepository.save(program);
    }

    public List<Program> getAllPrograms() {
        return programRepository.findAll();
    }

    public Program getProgramById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program không tồn tại"));
    }

    public Program updateProgram(Long id, Program updatedProgram) {
        Program existing = getProgramById(id);

        existing.setProgramName(updatedProgram.getProgramName());
        existing.setDateCreate(updatedProgram.getDateCreate());
        existing.setDateEnd(updatedProgram.getDateEnd());
        existing.setDescription(updatedProgram.getDescription());

        return programRepository.save(existing);
    }

    public void deleteProgram(Long id) {
        if (!programRepository.existsById(id)) {
            throw new RuntimeException("Program không tồn tại");
        }
        programRepository.deleteById(id);
    }
}
