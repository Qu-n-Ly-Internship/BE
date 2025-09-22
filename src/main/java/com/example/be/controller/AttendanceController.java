package com.example.be.controller;

import com.example.be.entity.Attendance;
import com.example.be.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;

    // Check-in
    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> request) {
        try {
            Long internId = Long.valueOf(request.get("internId").toString());
            String method = request.get("method").toString();

            Attendance attendance = Attendance.builder()
                    .internId(internId)
                    .checkInTime(LocalDateTime.now())
                    .method(method)
                    .status("CHECKED_IN")
                    .build();

            Attendance savedAttendance = attendanceRepository.save(attendance);
            return ResponseEntity.ok(savedAttendance);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Check-in thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Check-out
    @PutMapping("/checkout/{attendanceId}")
    public ResponseEntity<?> checkOut(@PathVariable Long attendanceId) {
        try {
            var attendanceOpt = attendanceRepository.findById(attendanceId);
            if (attendanceOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy bản ghi attendance");
                return ResponseEntity.notFound().build();
            }

            Attendance attendance = attendanceOpt.get();
            attendance.setCheckOutTime(LocalDateTime.now());
            attendance.setStatus("CHECKED_OUT");

            Attendance updatedAttendance = attendanceRepository.save(attendance);
            return ResponseEntity.ok(updatedAttendance);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Check-out thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy lịch sử attendance theo intern
    @GetMapping("/intern/{internId}")
    public ResponseEntity<?> getAttendanceByIntern(@PathVariable Long internId) {
        try {
            List<Attendance> attendances = attendanceRepository.findByInternId(internId);
            return ResponseEntity.ok(attendances);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải lịch sử attendance: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy tất cả attendance records
    @GetMapping
    public ResponseEntity<?> getAllAttendance() {
        try {
            List<Attendance> attendances = attendanceRepository.findAll();
            return ResponseEntity.ok(attendances);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách attendance: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}