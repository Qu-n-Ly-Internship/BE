package com.example.be.controller;

import com.example.be.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    // 1. Lấy danh sách tất cả mentor (users có role MENTOR hoặc HR)
    @GetMapping("")
    public ResponseEntity<?> getAllMentors(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "available", required = false) Boolean available
    ) {
        try {
            Map<String, Object> result = mentorService.getAllMentors(query, department, available);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 2. Lấy thông tin mentor được chọn cho thực tập sinh
    @GetMapping("/by-intern/{internId}")
    public ResponseEntity<?> getMentorByIntern(@PathVariable Long internId) {
        try {
            Map<String, Object> result = mentorService.getMentorByIntern(internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 3. Phân công mentor cho thực tập sinh
    @PostMapping("/assign")
    public ResponseEntity<?> assignMentor(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = mentorService.assignMentor(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 4. Lưu thông tin mentor được chọn cho thực tập sinh
    @PutMapping("/save-selection")
    public ResponseEntity<?> saveMentorSelection(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = mentorService.saveMentorSelection(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 5. Kiểm tra mentor của thực tập sinh có được chọn ko
    @GetMapping("/check-assignment/{internId}")
    public ResponseEntity<?> checkMentorAssignment(@PathVariable Long internId) {
        try {
            Map<String, Object> result = mentorService.checkMentorAssignment(internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 6. Lấy danh sách thực tập sinh chưa có mentor
    @GetMapping("/unassigned-interns")
    public ResponseEntity<?> getUnassignedInterns() {
        try {
            Map<String, Object> result = mentorService.getUnassignedInterns();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 7. Truy xuất danh sách đã phân công
    @GetMapping("/assignments")
    public ResponseEntity<?> getAllAssignments(
            @RequestParam(value = "mentorId", required = false) Long mentorId
    ) {
        try {
            Map<String, Object> result = mentorService.getAllAssignments(mentorId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 8. Xóa phân công mentor
    @DeleteMapping("/unassign")
    public ResponseEntity<?> unassignMentor(
            @RequestParam Long mentorId,
            @RequestParam Long internId
    ) {
        try {
            Map<String, Object> result = mentorService.unassignMentor(mentorId, internId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 9. Lấy thống kê phân công mentor
    @GetMapping("/stats")
    public ResponseEntity<?> getMentorStats() {
        try {
            Map<String, Object> result = mentorService.getMentorStats();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}