package com.example.be.controller;

import com.example.be.service.InternService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
public class Filter_InternController {

    private final InternService internService;

    // 1. Tìm kiếm và lọc thực tập sinh
    @GetMapping("")
    public ResponseEntity<?> searchInterns(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "university", required = false) String university,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "program", required = false) String program,
            @RequestParam(value = "yearOfStudy", required = false) Integer yearOfStudy,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Map<String, Object> result = internService.searchInterns(query, university, major, program, yearOfStudy, status, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tìm kiếm thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 2. Lấy chi tiết một thực tập sinh
    @GetMapping("/{id}")
    public ResponseEntity<?> getInternById(@PathVariable Long id) {
        try {
            Map<String, Object> result = internService.getInternById(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết thực tập sinh: " + e.getMessage()
            ));
        }
    }

    // 3. Thống kê thực tập sinh
    @GetMapping("/stats")
    public ResponseEntity<?> getInternStats() {
        try {
            Map<String, Object> result = internService.getInternStats();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    // 4. Lấy danh sách trường để filter
    @GetMapping("/universities")
    public ResponseEntity<?> getUniversities() {
        try {
            Map<String, Object> result = internService.getUniversities();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách trường: " + e.getMessage()
            ));
        }
    }

    // 5. Lấy danh sách ngành để filter
    @GetMapping("/majors")
    public ResponseEntity<?> getMajors() {
        try {
            Map<String, Object> result = internService.getMajors();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách ngành: " + e.getMessage()
            ));
        }
    }
}