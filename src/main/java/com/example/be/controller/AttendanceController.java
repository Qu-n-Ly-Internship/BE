package com.example.be.controller;

import com.example.be.dto.AttendanceHistoryDTO;
import com.example.be.dto.AttendanceRecordDTO;
import com.example.be.dto.AttendanceReportDTO;
import com.example.be.dto.ResponseWrapper;
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

    // 1️⃣ API tạo QR code cho buổi làm việc
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

    // 4️⃣ API lấy records của thực tập sinh hiện tại
    @GetMapping("/records/my")
    public ResponseEntity<List<AttendanceRecord>> getMyAttendanceRecords(
            @RequestParam Long userId
    ) {
        List<AttendanceRecord> records = attendanceService.getRecordsForCurrentIntern(userId);
        return ResponseEntity.ok(records);
    }

    // 5️⃣ API Check-in thủ công
    @PostMapping("/check-in")
    public ResponseEntity<ResponseWrapper<AttendanceRecordDTO>> checkIn(
            @RequestBody CheckInOutRequest request
    ) {
        try {
            AttendanceRecordDTO record = attendanceService.checkIn(request.getUserId());
            return ResponseEntity.ok(ResponseWrapper.success(record, "Check-in thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // 6️⃣ API Check-out thủ công
    @PostMapping("/check-out")
    public ResponseEntity<ResponseWrapper<AttendanceRecordDTO>> checkOut(
            @RequestBody CheckInOutRequest request
    ) {
        try {
            AttendanceRecordDTO record = attendanceService.checkOut(request.getUserId());
            return ResponseEntity.ok(ResponseWrapper.success(record, "Check-out thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // 7️⃣ API lấy thông tin chấm công hôm nay
    @GetMapping("/today")
    public ResponseEntity<ResponseWrapper<AttendanceRecordDTO>> getTodayAttendance(
            @RequestParam Long userId,
            @RequestParam String date
    ) {
        LocalDate queryDate = LocalDate.parse(date);
        AttendanceRecordDTO record = attendanceService.getTodayAttendance(userId, queryDate);
        return ResponseEntity.ok(ResponseWrapper.success(record, "Lấy thông tin thành công"));
    }

    // 8️⃣ API lấy lịch sử chấm công (có phân trang)
    @GetMapping("/history")
    public ResponseEntity<ResponseWrapper<List<AttendanceRecordDTO>>> getAttendanceHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AttendanceHistoryDTO history = attendanceService.getAttendanceHistory(userId, page, size);
        return ResponseEntity.ok(ResponseWrapper.success(history.getData(), "Lấy lịch sử thành công"));
    }

    // 9️⃣ API lấy báo cáo chuyên cần (cho HR/Admin)
    @GetMapping("/report")
    public ResponseEntity<ResponseWrapper<List<AttendanceReportDTO>>> getAttendanceReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) String search
    ) {
        List<AttendanceReportDTO> report = attendanceService.getAttendanceReport(
                startDate, endDate, department, mentorId, search
        );
        return ResponseEntity.ok(ResponseWrapper.success(report, "Lấy báo cáo thành công"));
    }

    // DTO cho request check-in/check-out
    @lombok.Data
    public static class CheckInOutRequest {
        private Long userId;
    }
}