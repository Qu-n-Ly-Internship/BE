package com.example.be.controller;

import com.example.be.entity.AttendanceRecord;
import com.example.be.service.Attendance.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 1️⃣ API tạo QR code cho buổi làm việc (ví dụ HR hoặc hệ thống gọi)
    @GetMapping("/generate")
    public ResponseEntity<String> generateQrCode(@RequestParam String code) {
        String qrUrl = attendanceService.generateQrCode(code);
        return ResponseEntity.ok(qrUrl);
    }

    // 2️⃣ API khi thực tập sinh quét QR
    @PostMapping("/scan")
    public ResponseEntity<String> processQrScan(
            @RequestParam Long userId,
            @RequestParam String code,
            @RequestParam String sig
    ) {
        String message = attendanceService.processQrScan(userId, code, sig);
        return ResponseEntity.ok(message);
    }

    // 3️⃣ API cho HR xem danh sách chấm công theo ngày
    @GetMapping("/records/date")
    public ResponseEntity<List<AttendanceRecord>> getRecordsByDate(
            @RequestParam(required = false) String date
    ) {
        LocalDate queryDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        List<AttendanceRecord> records = attendanceService.getRecordsByDate(queryDate);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/records/my")
    public ResponseEntity<List<AttendanceRecord>> getMyAttendanceRecords(
            @RequestParam Long userId
    ) {
        List<AttendanceRecord> records = attendanceService.getRecordsForCurrentIntern(userId);
        return ResponseEntity.ok(records);
    }
}
