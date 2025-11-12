package com.example.be.service.Attendance;

import com.example.be.dto.AttendanceHistoryDTO;
import com.example.be.dto.AttendanceRecordDTO;
import com.example.be.dto.AttendanceReportDTO;
import com.example.be.entity.AttendanceLog;
import com.example.be.entity.AttendanceRecord;
import com.example.be.entity.InternProfile;
import com.example.be.repository.*;
import com.example.be.service.InternContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository recordRepo;
    private final AttendanceLogRepository logRepo;
    private final InternContextService internContextService;
    private final InternProfileRepository internRepository;

    private static final String SECRET_KEY = "HMAC-key-local-dev-58f1c0b93aa94b2b";

    // 🔐 Utility để tạo HMAC chữ ký QR
    private String hmacSha256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKey);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }

    // ✅ 1. Tạo QR Code ký bằng HMAC
    public String generateQrCode(String code) {
        String signature = hmacSha256(code, SECRET_KEY);
        return "https://localhost:8090/api/attendance/scan?code=" + code + "&sig=" + signature;
    }

    // ✅ 2. Xử lý quét QR → check-in / check-out
    public String processQrScan(Long userId, String code, String sig) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));

        String expectedSig = hmacSha256(code, SECRET_KEY);
        if (!expectedSig.equals(sig)) {
            logRepo.save(AttendanceLog.builder()
                    .intern(intern)
                    .eventType(AttendanceLog.EventType.CHECKIN)
                    .payload(code)
                    .sig(sig)
                    .build());
            throw new IllegalArgumentException("Invalid QR signature!");
        }

        LocalDate today = LocalDate.now();
        AttendanceRecord record = recordRepo.findByInternIdAndWorkDate(internId, today).orElse(null);

        if (record == null) {
            record = new AttendanceRecord();
            record.setIntern(intern);
            record.setWorkDate(today);
            record.setCheckInTime(LocalDateTime.now());
            record.setMethod("QR");
            record.setStatus("present");
            recordRepo.save(record);

            logRepo.save(AttendanceLog.builder()
                    .intern(intern)
                    .eventType(AttendanceLog.EventType.CHECKIN)
                    .payload(code)
                    .sig(sig)
                    .build());
            return "✅ Checked in successfully";
        } else if (record.getCheckOutTime() == null) {
            record.setCheckOutTime(LocalDateTime.now());
            recordRepo.save(record);

            logRepo.save(AttendanceLog.builder()
                    .intern(intern)
                    .eventType(AttendanceLog.EventType.CHECKOUT)
                    .payload(code)
                    .sig(sig)
                    .build());
            return "✅ Checked out successfully";
        } else {
            logRepo.save(AttendanceLog.builder()
                    .intern(intern)
                    .eventType(AttendanceLog.EventType.EXPIRED)
                    .payload(code)
                    .sig(sig)
                    .notes("Already checked out today")
                    .build());
            return "⚠️ Already checked out today";
        }
    }

    // ✅ 3. HR / Admin lấy danh sách chấm công theo ngày
    public List<AttendanceRecord> getRecordsByDate(LocalDate date) {
        return recordRepo.findAllByWorkDate(date);
    }

    // ✅ 4. Lấy records của thực tập sinh hiện tại
    public List<AttendanceRecord> getRecordsForCurrentIntern(Long userId) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        return recordRepo.findByInternId(internId);
    }

    // ✅ 5. Check-in thủ công
    public AttendanceRecordDTO checkIn(Long userId) {
        try {
            System.out.println("=== CHECK-IN START ===");
            System.out.println("User ID: " + userId);
            
            Long internId = internContextService.getInternIdFromUserId(userId);
            System.out.println("Intern ID: " + internId);
            
            if (internId == null) {
                throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
            }
            
            InternProfile intern = internRepository.findById(internId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));
            System.out.println("Intern found: " + intern.getId());

            LocalDate today = LocalDate.now();
            System.out.println("Today: " + today);
            
            // Kiểm tra đã check-in chưa
            Optional<AttendanceRecord> existingRecord = recordRepo.findByInternIdAndWorkDate(internId, today);
            if (existingRecord.isPresent()) {
                System.out.println("Already checked in!");
                throw new IllegalArgumentException("Bạn đã check-in hôm nay rồi!");
            }

            // Tạo record mới
            AttendanceRecord record = new AttendanceRecord();
            record.setIntern(intern);
            record.setWorkDate(today);
            record.setCheckInTime(LocalDateTime.now());
            record.setMethod("MANUAL");
            record.setStatus("present");
            
            System.out.println("Saving record...");
            record = recordRepo.save(record);
            System.out.println("Record saved: " + record.getId());

            // Log event - Tạo object thủ công thay vì dùng Builder
            try {
                AttendanceLog log = new AttendanceLog();
                log.setIntern(intern);
                log.setEventType(AttendanceLog.EventType.CHECKIN);
                log.setEventTime(LocalDateTime.now());
                log.setPayload("Manual check-in");
                
                logRepo.save(log);
                System.out.println("Log saved");
            } catch (Exception e) {
                System.err.println("Failed to save log: " + e.getMessage());
                e.printStackTrace();
                // Continue even if log fails
            }

            System.out.println("=== CHECK-IN SUCCESS ===");
            return convertToDTO(record);
        } catch (Exception e) {
            System.err.println("=== CHECK-IN ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ✅ 6. Check-out thủ công
    public AttendanceRecordDTO checkOut(Long userId) {
        try {
            System.out.println("=== CHECK-OUT START ===");
            System.out.println("User ID: " + userId);
            
            Long internId = internContextService.getInternIdFromUserId(userId);
            System.out.println("Intern ID: " + internId);
            
            if (internId == null) {
                throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
            }
            
            InternProfile intern = internRepository.findById(internId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));
            System.out.println("Intern found: " + intern.getId());

            LocalDate today = LocalDate.now();
            System.out.println("Today: " + today);
            
            // Tìm record hôm nay
            AttendanceRecord record = recordRepo.findByInternIdAndWorkDate(internId, today)
                    .orElseThrow(() -> new IllegalArgumentException("Bạn chưa check-in hôm nay!"));

            // Kiểm tra đã check-out chưa
            if (record.getCheckOutTime() != null) {
                System.out.println("Already checked out!");
                throw new IllegalArgumentException("Bạn đã check-out hôm nay rồi!");
            }

            // Update check-out time
            record.setCheckOutTime(LocalDateTime.now());
            System.out.println("Updating record...");
            record = recordRepo.save(record);
            System.out.println("Record updated: " + record.getId());

            // Log event - Tạo object thủ công
            try {
                AttendanceLog log = new AttendanceLog();
                log.setIntern(intern);
                log.setEventType(AttendanceLog.EventType.CHECKOUT);
                log.setEventTime(LocalDateTime.now());
                log.setPayload("Manual check-out");
                
                logRepo.save(log);
                System.out.println("Log saved");
            } catch (Exception e) {
                System.err.println("Failed to save log: " + e.getMessage());
                e.printStackTrace();
                // Continue even if log fails
            }

            System.out.println("=== CHECK-OUT SUCCESS ===");
            return convertToDTO(record);
        } catch (Exception e) {
            System.err.println("=== CHECK-OUT ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ✅ 7. Lấy thông tin chấm công hôm nay
    public AttendanceRecordDTO getTodayAttendance(Long userId, LocalDate date) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }

        AttendanceRecord record = recordRepo.findByInternIdAndWorkDate(internId, date).orElse(null);
        return record != null ? convertToDTO(record) : null;
    }

    // ✅ 8. Lấy lịch sử chấm công với phân trang
    public AttendanceHistoryDTO getAttendanceHistory(Long userId, int page, int size) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<AttendanceRecord> recordPage = recordRepo.findByInternIdOrderByWorkDateDesc(internId, pageable);

        List<AttendanceRecordDTO> records = recordPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        AttendanceHistoryDTO historyDTO = new AttendanceHistoryDTO();
        historyDTO.setData(records);
        historyDTO.setTotalElements(recordPage.getTotalElements());
        historyDTO.setTotalPages(recordPage.getTotalPages());
        historyDTO.setCurrentPage(recordPage.getNumber());
        
        return historyDTO;
    }

    // ✅ 9. Lấy báo cáo chuyên cần (cho HR/Admin)
    public List<AttendanceReportDTO> getAttendanceReport(
            String startDate, 
            String endDate, 
            String department, 
            Long mentorId, 
            String search
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now();

        // Lấy tất cả records trong khoảng thời gian
        List<AttendanceRecord> allRecords = recordRepo.findAllByWorkDateBetween(start, end);

        // Group by intern và tính toán thống kê
        Map<Long, List<AttendanceRecord>> groupedByIntern = allRecords.stream()
                .collect(Collectors.groupingBy(record -> record.getIntern().getId()));

        List<AttendanceReportDTO> reportList = groupedByIntern.entrySet().stream()
                .map(entry -> {
                    Long internId = entry.getKey();
                    List<AttendanceRecord> records = entry.getValue();
                    InternProfile intern = records.get(0).getIntern();

                    // Tính toán các chỉ số
                    long workingDays = records.stream()
                            .filter(r -> r.getCheckInTime() != null)
                            .count();

                    long lateDays = records.stream()
                            .filter(r -> {
                                if (r.getCheckInTime() == null) return false;
                                // Coi là muộn nếu check-in sau 8:30
                                return r.getCheckInTime().toLocalTime().isAfter(java.time.LocalTime.of(8, 30));
                            })
                            .count();

                    long absentDays = records.stream()
                            .filter(r -> "absent".equals(r.getStatus()))
                            .count();

                    // Lấy department - xử lý nếu không có field department trong InternProfile
                    String deptName = "Chưa phân công"; // Default value
                    // TODO: Nếu InternProfile có field department, uncomment dòng dưới:
                    // deptName = intern.getDepartment() != null ? intern.getDepartment() : "Chưa phân công";

                    return AttendanceReportDTO.builder()
                            .internId(internId)
                            .internName(intern.getUser().getFullName())
                            .employeeId("TTS" + internId)
                            .department(deptName)
                            .totalWorkingDays(workingDays)
                            .totalLeaveDays(0L) // Chưa implement leave days tracking
                            .totalLateDays(lateDays)
                            .totalAbsentDays(absentDays)
                            .build();
                })
                .collect(Collectors.toList());

        // Filter theo department nếu có
        if (department != null && !department.isEmpty()) {
            reportList = reportList.stream()
                    .filter(dto -> department.equals(dto.getDepartment()))
                    .collect(Collectors.toList());
        }

        // Filter theo search text nếu có
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            reportList = reportList.stream()
                    .filter(dto -> 
                            dto.getInternName().toLowerCase().contains(searchLower) ||
                            dto.getEmployeeId().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return reportList;
    }

    // ✅ Helper: Convert entity to DTO
    private AttendanceRecordDTO convertToDTO(AttendanceRecord record) {
        AttendanceRecordDTO dto = new AttendanceRecordDTO();
        dto.setId(record.getId());
        dto.setDate(record.getWorkDate());
        dto.setCheckInTime(record.getCheckInTime());
        dto.setCheckOutTime(record.getCheckOutTime());
        dto.setMethod(record.getMethod());
        dto.setStatus(record.getStatus());
        return dto;
    }
}