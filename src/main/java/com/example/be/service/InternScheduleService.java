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

            LocalDate start = program.getStartDate();
            LocalDate end = program.getEndDate();

            List<InternSchedule> schedules = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                schedules.add(InternSchedule.builder()
                        .intern(intern)
                        .program(program)
                        .date(date)
                        .status("PLANNED")
                        .note("Làm việc theo chương trình " + program.getTitle())
                        .build());
            }

            scheduleRepository.saveAll(schedules);
            return Map.of("success", true, "message", "Đã sinh lịch tự động", "data", schedules);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
