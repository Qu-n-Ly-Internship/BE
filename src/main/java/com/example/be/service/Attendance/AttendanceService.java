package com.example.be.service.Attendance;

import com.example.be.dto.AttendanceHistoryDTO;
import com.example.be.dto.AttendanceRecordDTO;
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
import java.util.stream.Collectors;

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
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));

        LocalDate today = LocalDate.now();
        
        // Kiểm tra đã check-in chưa
        AttendanceRecord existingRecord = recordRepo.findByInternIdAndWorkDate(internId, today).orElse(null);
        if (existingRecord != null) {
            throw new IllegalArgumentException("Bạn đã check-in hôm nay rồi!");
        }

        // Tạo record mới
        AttendanceRecord record = new AttendanceRecord();
        record.setIntern(intern);
        record.setWorkDate(today);
        record.setCheckInTime(LocalDateTime.now());
        record.setMethod("MANUAL");
        record.setStatus("present");
        record = recordRepo.save(record);

        // Log event
        logRepo.save(AttendanceLog.builder()
                .intern(intern)
                .eventType(AttendanceLog.EventType.CHECKIN)
                .payload("Manual check-in")
                .build());

        return convertToDTO(record);
    }

    // ✅ 6. Check-out thủ công
    public AttendanceRecordDTO checkOut(Long userId) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));

        LocalDate today = LocalDate.now();
        
        // Tìm record hôm nay
        AttendanceRecord record = recordRepo.findByInternIdAndWorkDate(internId, today)
                .orElseThrow(() -> new IllegalArgumentException("Bạn chưa check-in hôm nay!"));

        // Kiểm tra đã check-out chưa
        if (record.getCheckOutTime() != null) {
            throw new IllegalArgumentException("Bạn đã check-out hôm nay rồi!");
        }

        // Update check-out time
        record.setCheckOutTime(LocalDateTime.now());
        record = recordRepo.save(record);

        // Log event
        logRepo.save(AttendanceLog.builder()
                .intern(intern)
                .eventType(AttendanceLog.EventType.CHECKOUT)
                .payload("Manual check-out")
                .build());

        return convertToDTO(record);
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