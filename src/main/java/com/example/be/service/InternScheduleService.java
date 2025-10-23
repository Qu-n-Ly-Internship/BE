package com.example.be.service;

import com.example.be.entity.*;
import com.example.be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InternScheduleService {

    private final InternRepository internRepository;
    private final InternScheduleRepository scheduleRepository;
    private final TaskRepository taskRepository; // cần tạo nếu chưa có

    // Lấy lịch cá nhân
    public Map<String, Object> getScheduleByIntern(Long internId) {
        try {
            List<InternSchedule> schedules = scheduleRepository.findByIntern_Id(internId);
            return Map.of("success", true, "data", schedules);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Lỗi khi lấy lịch: " + e.getMessage());
        }
    }

    // Sinh lịch tự động từ chương trình
    public Map<String, Object> generateSchedule(Long internId) {
        try {
            InternProfile intern = internRepository.findById(internId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh"));

            InternProgram program = intern.getProgram();
            if (program == null) {
                throw new RuntimeException("Thực tập sinh chưa tham gia chương trình nào");
            }

            InternSchedule schedule = InternSchedule.builder()
                    .intern(intern)
                    .program(program)
                    .date(LocalDate.now())
                    .status("PLANNED")
                    .note("Lịch mặc định cho chương trình " + program.getTitle())
                    .build();

            scheduleRepository.save(schedule);

            return Map.of("success", true, "message", "Đã sinh 1 lịch mặc định", "data", schedule);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
