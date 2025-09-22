package com.example.be.controller;

import com.example.be.entity.Task;
import com.example.be.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;

    // Lấy tất cả tasks
    @GetMapping
    public ResponseEntity<?> getAllTasks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        try {
            if (status != null && !status.isEmpty()) {
                List<Task> tasks = taskRepository.findByStatus(status);
                return ResponseEntity.ok(tasks);
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Task> tasks = taskRepository.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", tasks.getContent());
            response.put("totalElements", tasks.getTotalElements());
            response.put("totalPages", tasks.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách task: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Tạo task mới
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        try {
            task.setCreatedAt(LocalDateTime.now());
            if (task.getStatus() == null) {
                task.setStatus("TODO");
            }

            Task savedTask = taskRepository.save(task);
            return ResponseEntity.ok(savedTask);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo task: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy tasks theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getTasksByIntern(@PathVariable Long internId) {
        try {
            List<Task> tasks = taskRepository.findByAssignedTo(internId);
            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải task của intern: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Cập nhật task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task task) {
        try {
            if (!taskRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy task với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            task.setTaskId(id);
            Task updatedTask = taskRepository.save(task);
            return ResponseEntity.ok(updatedTask);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật task: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Cập nhật status task
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            var taskOpt = taskRepository.findById(id);
            if (taskOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy task với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            Task task = taskOpt.get();
            task.setStatus(request.get("status"));
            Task updatedTask = taskRepository.save(task);

            return ResponseEntity.ok(updatedTask);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật trạng thái task: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Xóa task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            if (!taskRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy task với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            taskRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa task thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa task: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}