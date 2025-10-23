package com.example.be.controller;

import com.example.be.service.InternshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class ProjectController {
    private final InternshipService internshipService;

    // 1. Lấy danh sách tất cả intern programs với filter
    @GetMapping("")
    public ResponseEntity<?> getAllInternships(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            Map<String, Object> result = internshipService.getAllInternships(query, startDate, endDate, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách chương trình: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy chi tiết một intern program
    @GetMapping("/{id}")
    public ResponseEntity<?> getProgramById(@PathVariable Long id) {
        try {
            Map<String, Object> result = internshipService.getProgramById(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết chương trình: " + e.getMessage()
            ));
        }
    }

    // 3. Tạo intern program mới
    @PostMapping("")
    public ResponseEntity<?> createProgram(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = internshipService.createProgram(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo chương trình: " + e.getMessage()
            ));
        }
    }

    // 4. Cập nhật intern program
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProgram(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Map<String, Object> result = internshipService.updateProgram(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật chương trình: " + e.getMessage()
            ));
        }
    }

    // 5. Xóa intern program
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable Long id) {
        try {
            internshipService.deleteProgram(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa chương trình thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi xóa chương trình: " + e.getMessage()
            ));
        }
    }

    // 6. Lấy danh sách thực tập sinh theo chương trình
    @GetMapping("/{id}/interns")
    public ResponseEntity<?> getInternsByProgram(@PathVariable Long id) {
        try {
            Map<String, Object> result = internshipService.getInternsByProgram(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách thực tập sinh: " + e.getMessage()
            ));
        }
    }
}