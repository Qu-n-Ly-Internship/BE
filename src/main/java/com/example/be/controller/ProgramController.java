package com.example.be.controller;

import com.example.be.dto.ProgramRequest;
import com.example.be.entity.Program;
import com.example.be.service.ProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    // ✅ HR tạo chương trình
    @PostMapping
    public ResponseEntity<?> createProgram(@RequestBody ProgramRequest request) {
        try {
            Program program = new Program();
            program.setProgramName(request.getProgramName());
            program.setDateCreate(request.getDateCreate());
            program.setDateEnd(request.getDateEnd());
            program.setDescription(request.getDescription());

            Program saved = programService.createProgram(program, request.getHrId());
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<ProgramRequest> getAllPrograms() {
        return programService.getAllPrograms();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Program> getById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    // ✅ Cập nhật
    @PutMapping("/{id}")
    public ResponseEntity<Program> update(@PathVariable Long id, @RequestBody Program program) {
        return ResponseEntity.ok(programService.updateProgram(id, program));
    }

    // ✅ Xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
}
