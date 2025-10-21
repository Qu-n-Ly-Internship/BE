package com.example.be.service;

import com.example.be.entity.*;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final InternRepository internRepository;
    private final InternScheduleRepository scheduleRepository;

    // Lấy danh sách task theo intern
    public Map<String, Object> getTasksByIntern(Long internId) {
        try {
            List<Task> tasks = taskRepository.findByAssignedTo_Id(internId);
            return Map.of("success", true, "data", tasks);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi lấy task: " + e.getMessage());
        }
    }

    // Giao công việc mới
    public Map<String, Object> assignTask(Map<String, Object> req) {
        try {
            Long internId = Long.valueOf(req.get("internId").toString());
            String title = (String) req.get("title");
            String description = (String) req.get("description");
            String priority = (String) req.get("priority");
            String dueDate = (String) req.get("dueDate");
            Long assignedBy = Long.valueOf(req.get("assignedBy").toString());

            InternProfile intern = internRepository.findById(internId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh"));

            User user = new User();
            user.setId(assignedBy);

            Task task = Task.builder()
                    .title(title)
                    .description(description)
                    .priority(priority)
                    .status("NEW")
                    .createdAt(LocalDateTime.now())
                    .dueDate(java.time.LocalDate.parse(dueDate))
                    .assignedTo(intern)
                    .assignedBy(user)
                    .build();

            taskRepository.save(task);

            // ✅ Tự động tạo lịch làm việc tương ứng
            InternSchedule schedule = InternSchedule.builder()
                    .intern(intern)
                    .program(intern.getProgram())
                    .task(task)
                    .date(task.getDueDate())
                    .status("PLANNED")
                    .note("Thực hiện công việc: " + task.getTitle())
                    .build();
            scheduleRepository.save(schedule);

            return Map.of("success", true, "message", "Giao việc thành công", "data", task);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi giao việc: " + e.getMessage());
        }
    }

    // Cập nhật trạng thái công việc
    public Map<String, Object> updateTaskStatus(Long taskId, String status) {
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));
            task.setStatus(status);
            taskRepository.save(task);

            // Cập nhật lịch tương ứng
            scheduleRepository.findAll().stream()
                    .filter(s -> s.getTask() != null && Objects.equals(s.getTask().getId(), taskId))
                    .forEach(s -> {
                        s.setStatus(status.equals("COMPLETED") ? "DONE" : "PLANNED");
                        scheduleRepository.save(s);
                    });

            return Map.of("success", true, "message", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}
