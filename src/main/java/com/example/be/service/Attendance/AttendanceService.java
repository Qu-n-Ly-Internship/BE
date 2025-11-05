package com.example.be.service.Attendance;


import com.example.be.entity.AttendanceLog;
import com.example.be.entity.AttendanceRecord;
import com.example.be.entity.InternProfile;
import com.example.be.repository.*;
import com.example.be.service.InternContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static AttendanceService HmacUtil;
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
        // ✅ 1. Từ userId -> internId
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy intern với id: " + internId));


        // ✅ 2. Xác minh chữ ký QR
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

        // ✅ 3. Lấy hoặc tạo record
        LocalDate today = LocalDate.now();
        AttendanceRecord record = recordRepo.findByInternIdAndWorkDate(internId, today).orElse(null);


        // ✅ 4. Check-in hoặc Check-out
        if (record == null) {
            // --- Check-in ---
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
            // --- Check-out ---
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
            // --- Đã checkout ---
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

    public List<AttendanceRecord> getRecordsForCurrentIntern(Long userId) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new IllegalArgumentException("User này không có hồ sơ thực tập sinh!");
        }
        return recordRepo.findByInternId(internId);
    }


}
