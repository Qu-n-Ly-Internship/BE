package com.example.be.controller;

import com.example.be.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ✅ Lấy tasks của user hiện tại (dựa trên userId) - Cho INTERN
    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(taskService.getMyTasks(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // ✅ Lấy lịch thực tập từ tasks (dựa trên userId) - Cho INTERN
    @GetMapping("/my-schedule")
    public ResponseEntity<?> getMySchedule(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(taskService.getMySchedule(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // ✅ Lấy danh sách tasks đã giao (cho MENTOR)
    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedTasks(@RequestParam Long mentorUserId) {
        try {
            return ResponseEntity.ok(taskService.getAssignedTasks(mentorUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Lấy danh sách công việc theo intern_id (cho admin/mentor)
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getTasksByIntern(@PathVariable Long internId) {
        return ResponseEntity.ok(taskService.getTasksByIntern(internId));
    }

    // ✅ Giao công việc mới (cho MENTOR)
    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(taskService.assignTask(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // Cập nhật trạng thái công việc
    @PutMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam String status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status));
    }
}