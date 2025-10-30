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

    // ✅ Lấy tasks của user hiện tại (dựa trên userId)
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

    // Lấy danh sách công việc theo intern_id (cho admin/mentor)
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getTasksByIntern(@PathVariable Long internId) {
        return ResponseEntity.ok(taskService.getTasksByIntern(internId));
    }

    // Giao công việc mới
    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(taskService.assignTask(request));
    }

    // Cập nhật trạng thái công việc
    @PutMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam String status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status));
    }
}