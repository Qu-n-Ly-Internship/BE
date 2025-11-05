package com.example.be.service;

import com.example.be.entity.*;
import com.example.be.notification.service.NotificationPublisher;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final InternRepository internRepository;
    private final InternScheduleRepository scheduleRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository; // ✅ Thêm MentorRepository
    private final JdbcTemplate jdbcTemplate;
    private final NotificationPublisher notificationPublisher;

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
                        t.assigned_by,
                        m.fullname as mentor_name
                    FROM task t
                    LEFT JOIN mentors m ON t.assigned_by = m.mentor_id
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
                formattedTask.put("mentorName", task.get("mentor_name")); // ✅ Thêm tên mentor
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

    // ✅ Lấy lịch thực tập từ tasks (dùng created_at và due_date)
    public Map<String, Object> getMySchedule(Long userId) {
        try {
            Long internId = getInternIdByUserId(userId);

            String sql = """
                    SELECT 
                        t.task_id,
                        t.title as task,
                        t.description,
                        t.priority,
                        t.status,
                        t.created_at as start_date,
                        t.due_date as end_date,
                        ip.fullname as intern_name,
                        m.fullname as mentor_name,
                        'Thực tập' as department
                    FROM task t
                    LEFT JOIN intern_profiles ip ON t.assigned_to = ip.intern_id
                    LEFT JOIN mentors m ON t.assigned_by = m.mentor_id
                    WHERE t.assigned_to = ?
                    ORDER BY t.created_at DESC
                    """;

            List<Map<String, Object>> schedule = jdbcTemplate.queryForList(sql, internId);

            // Chuyển đổi format cho frontend
            List<Map<String, Object>> formattedSchedule = new ArrayList<>();
            for (Map<String, Object> item : schedule) {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("id", item.get("task_id"));
                formatted.put("task", item.get("task"));
                formatted.put("description", item.get("description"));
                formatted.put("priority", item.get("priority"));
                formatted.put("status", item.get("status"));
                formatted.put("startDate", item.get("start_date"));
                formatted.put("endDate", item.get("end_date"));
                formatted.put("internName", item.get("intern_name"));
                formatted.put("mentorName", item.get("mentor_name")); // ✅ Thêm tên mentor
                formatted.put("department", item.get("department"));
                formattedSchedule.add(formatted);
            }

            return Map.of("success", true, "data", formattedSchedule);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi lấy lịch: " + e.getMessage());
        }
    }

    // ✅ Lấy danh sách tasks đã giao (cho mentor)
    public Map<String, Object> getAssignedTasks(Long mentorUserId) {
        try {
            // ✅ Tìm mentor_id từ user_id
            Mentors mentor = mentorRepository.findByUser_Id(mentorUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin mentor"));

            String sql = """
                    SELECT 
                        t.task_id,
                        t.title,
                        t.description,
                        t.priority,
                        t.status,
                        t.created_at,
                        t.due_date,
                        ip.fullname as intern_name,
                        ip.email as intern_email,
                        m.fullname as assigned_by_name
                    FROM task t
                    LEFT JOIN intern_profiles ip ON t.assigned_to = ip.intern_id
                    LEFT JOIN mentors m ON t.assigned_by = m.mentor_id
                    WHERE t.assigned_by = ?
                    ORDER BY t.created_at DESC
                    """;

            List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql, mentor.getId());

            // Format cho frontend
            List<Map<String, Object>> formattedTasks = new ArrayList<>();
            for (Map<String, Object> task : tasks) {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("id", task.get("task_id"));
                formatted.put("title", task.get("title"));
                formatted.put("description", task.get("description"));
                formatted.put("priority", task.get("priority"));
                formatted.put("status", task.get("status"));
                formatted.put("assignedAt", task.get("created_at"));
                formatted.put("duedate", task.get("due_date"));
                formatted.put("internName", task.get("intern_name"));
                formatted.put("internEmail", task.get("intern_email"));
                formatted.put("mentorName", task.get("assigned_by_name")); // ✅ Tên mentor đã giao
                formattedTasks.add(formatted);
            }

            return Map.of("success", true, "data", formattedTasks);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi lấy danh sách tasks đã giao: " + e.getMessage());
        }
    }

    // ✅ Giao công việc mới
    public Map<String, Object> assignTask(Map<String, Object> req) {
        try {
            Long internId = Long.valueOf(req.get("internId").toString());
            String title = (String) req.get("title");
            String description = (String) req.get("description");
            String priority = req.get("priority").toString();
            String dueDate = (String) req.get("dueDate");
            Long mentorUserId = Long.valueOf(req.get("assignedBy").toString());

            // ✅ Lấy thực tập sinh
            InternProfile intern = internRepository.findById(internId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh"));

            // ✅ Lấy mentor theo user_id
            Mentors mentor = mentorRepository.findByUser_Id(mentorUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin mentor với user_id: " + mentorUserId));

            // ✅ Tạo task
            Task task = Task.builder()
                    .title(title)
                    .description(description)
                    .priority(priority)
                    .status("NEW")
                    .createdAt(LocalDateTime.now())
                    .dueDate(LocalDate.parse(dueDate))
                    .assignedTo(intern)
                    .assignedBy(mentor)
                    .build();

            taskRepository.save(task);

            // ✅ Tự động tạo lịch thực tập (InternSchedule)
            if (intern.getProgram() != null) {
                InternSchedule schedule = InternSchedule.builder()
                        .intern(intern)
                        .program(intern.getProgram())
                        .task(task)
                        .date(task.getDueDate())
                        .status("PLANNED")
                        .title(task.getTitle()) // ⚠️ FIX QUAN TRỌNG: tránh lỗi "Column 'title' cannot be null"
                        .description(task.getDescription())
                        .note("Thực hiện công việc: " + task.getTitle())
                        .build();

                scheduleRepository.save(schedule);
            }
            try {
                // Lấy userId của intern để gửi notification
                Long internUserId = intern.getUser() != null ? intern.getUser().getId() : null;

                if (internUserId != null) {
                    String priorityEmoji = switch (priority) {
                        case "1" -> "🔴";
                        case "2" -> "🟡";
                        default -> "⚪";
                    };

                    String deadlineStr = LocalDate.parse(dueDate)
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    notificationPublisher.publish(
                            internUserId.toString(),                    // userId
                            "NEW_TASK",                                 // type
                            "📋 Task mới từ " + mentor.getFullName(),  // title
                            String.format(
                                    "%s %s - Deadline: %s\n%s",
                                    priorityEmoji,
                                    title,
                                    deadlineStr,
                                    description != null ? description : ""
                            )                                           // message
                    );

                    System.out.println("✅ Notification sent to intern userId: " + internUserId);
                }
            } catch (Exception e) {
                // Không throw exception để không ảnh hưởng việc tạo task
                System.err.println("❌ Failed to send notification: " + e.getMessage());
                e.printStackTrace();
            }
            return Map.of(
                    "success", true,
                    "message", "Giao việc thành công",
                    "data", Map.of(
                            "taskId", task.getId(),
                            "title", task.getTitle(),
                            "mentorName", mentor.getFullName()
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi giao việc: " + e.getMessage());
        }
    }


    // Cập nhật trạng thái công việc
    public Map<String, Object> updateTaskStatus(Long taskId, String newStatus) {

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));
            String oldStatus = task.getStatus();
            task.setStatus(newStatus);
            taskRepository.save(task);
            // Cập nhật lịch tương ứng (nếu có)
            try {
                scheduleRepository.findAll().stream()
                        .filter(s -> s.getTask() != null && Objects.equals(s.getTask().getId(), taskId))
                        .forEach(s -> {
                            s.setStatus(newStatus.equals("COMPLETED") ? "DONE" : "PLANNED");
                            scheduleRepository.save(s);
                        });
            } catch (Exception e) {
                // Bỏ qua lỗi nếu không có schedule
                System.out.println("No schedule to update: " + e.getMessage());
            }
            Mentors mentor = task.getAssignedBy();
            Long mentorUserId = mentor.getUser() != null ? mentor.getUser().getId() : null;

            if (mentorUserId != null) {
                String emoji = switch (task.getPriority()) {
                    case "1" -> "🔴";
                    case "2" -> "🟡";
                    default -> "⚪";
                };
                String deadline = task.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));

                String action = "IN_PROGRESS".equals(newStatus) ? "bắt đầu làm" : "HOÀN THÀNH";

                notificationPublisher.publish(
                        mentorUserId.toString(),
                        "TASK_UPDATED",
                        "Task được " + action,
                        String.format(
                                "%s %s\n" +
                                        "Cập nhật bởi: %s\n" +
                                        "Trạng thái: %s → %s\n" +
                                        "Deadline: %s",
                                emoji,
                                task.getTitle(),
                                task.getAssignedTo().getFullName(),
                                oldStatus, newStatus, deadline
                        )
                );
            }
            return Map.of("success", true, "message", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}