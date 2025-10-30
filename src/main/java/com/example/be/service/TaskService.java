package com.example.be.service;

import com.example.be.entity.*;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final InternRepository internRepository;
    private final InternScheduleRepository scheduleRepository;
    private final InternProfileRepository internProfileRepository;
    private final JdbcTemplate jdbcTemplate; // ✅ Thêm JdbcTemplate

    // ✅ Lấy intern_id từ user_id
    public Long getInternIdByUserId(Long userId) {
        try {
            String sql = "SELECT intern_id FROM intern_profiles WHERE user_id = ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);

            if (result.isEmpty()) {
                throw new RuntimeException("Không tìm thấy thông tin thực tập sinh với user_id: " + userId);
            }

            return ((Number) result.get(0).get("intern_id")).longValue();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm intern_id: " + e.getMessage());
        }
    }

    // ✅ Lấy danh sách task theo intern_id (dùng SQL thuần)
    public Map<String, Object> getTasksByIntern(Long internId) {
        try {
            String sql = """
                SELECT 
                    t.task_id,
                    t.title,
                    t.description,
                    t.priority,
                    t.status,
                    t.created_at,
                    t.due_date,
                    t.assigned_to,
                    t.assigned_by
                FROM task t
                WHERE t.assigned_to = ?
                ORDER BY t.created_at DESC
                """;

            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql, internId);

            // Chuyển đổi key từ snake_case sang camelCase
            List<Map<String, Object>> formattedTasks = new ArrayList<>();
            for (Map<String, Object> task : tasks) {
                Map<String, Object> formattedTask = new HashMap<>();
                formattedTask.put("id", task.get("task_id"));
                formattedTask.put("title", task.get("title"));
                formattedTask.put("description", task.get("description"));
                formattedTask.put("priority", task.get("priority"));
                formattedTask.put("status", task.get("status"));
                formattedTask.put("createdAt", task.get("created_at"));
                formattedTask.put("dueDate", task.get("due_date"));
                formattedTask.put("assignedTo", task.get("assigned_to"));
                formattedTask.put("assignedBy", task.get("assigned_by"));
                formattedTasks.add(formattedTask);
            }

            return Map.of("success", true, "data", formattedTasks);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi lấy task: " + e.getMessage());
        }
    }

    // ✅ Lấy task của user hiện tại
    public Map<String, Object> getMyTasks(Long userId) {
        try {
            Long internId = getInternIdByUserId(userId);
            return getTasksByIntern(internId);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi: " + e.getMessage());
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

            // ✅ Tự động tạo lịch làm việc tương ứng (nếu có)
            if (intern.getProgram() != null) {
                InternSchedule schedule = InternSchedule.builder()
                        .intern(intern)
                        .program(intern.getProgram())
                        .task(task)
                        .date(task.getDueDate())
                        .status("PLANNED")
                        .note("Thực hiện công việc: " + task.getTitle())
                        .build();
                scheduleRepository.save(schedule);
            }

            return Map.of("success", true, "message", "Giao việc thành công", "data", task);
        } catch (Exception e) {
            e.printStackTrace();
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

            // Cập nhật lịch tương ứng (nếu có)
            try {
                scheduleRepository.findAll().stream()
                        .filter(s -> s.getTask() != null && Objects.equals(s.getTask().getId(), taskId))
                        .forEach(s -> {
                            s.setStatus(status.equals("COMPLETED") ? "DONE" : "PLANNED");
                            scheduleRepository.save(s);
                        });
            } catch (Exception e) {
                // Bỏ qua lỗi nếu không có schedule
                System.out.println("No schedule to update: " + e.getMessage());
            }

            return Map.of("success", true, "message", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}