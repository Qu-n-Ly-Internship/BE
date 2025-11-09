// package com.example.be.Service;

// import com.example.be.entity.AttendanceLog;
// import com.example.be.entity.AttendanceRecord;
// import com.example.be.entity.InternProfile;
// import com.example.be.repository.AttendanceLogRepository;
// import com.example.be.repository.AttendanceRecordRepository;
// import com.example.be.repository.InternProfileRepository;
// import com.example.be.service.Attendance.AttendanceService;
// import com.example.be.service.InternContextService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// // Sử dụng RandomPort để đảm bảo môi trường test độc lập
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// // @Transactional để rollback các thay đổi DB sau mỗi test
// @Transactional
// public class AttendanceServiceTest {

//     @Autowired
//     private AttendanceService attendanceService;

//     @Autowired
//     private AttendanceRecordRepository recordRepo;

//     @Autowired
//     private AttendanceLogRepository logRepo;

//     @Autowired
//     private InternProfileRepository internProfileRepository; // Cần Repository để setup dữ liệu Intern

//     // Mock InternContextService vì nó là dependency bên ngoài
//     @MockBean
//     private InternContextService internContextService;

//     private final Long TEST_USER_ID = 99L;
//     private final Long TEST_INTERN_ID = 100L;
//     private InternProfile testIntern;

//     @BeforeEach
//     void setUp() {
//         // 1. Setup InternProfile trong DB thật (giả định có repo này)
//         testIntern = InternProfile.builder()
//                 .id(TEST_INTERN_ID)
//                 .fullName("Test Intern")
//                 .build();
//         // Lưu để đảm bảo ràng buộc khóa ngoại được thỏa mãn
//         internProfileRepository.save(testIntern);

//         // 2. Mock hành vi của InternContextService
//         when(internContextService.getInternIdFromUserId(TEST_USER_ID)).thenReturn(TEST_INTERN_ID);

//         // 3. Clear logs trước mỗi test (đảm bảo sạch)
//         logRepo.deleteAll();
//     }

//     // --- 1. Test generateQrCode ---
//     @Test
//     void testGenerateQrCode_shouldContainCodeAndValidSignature() {
//         String code = "random-code-123";
//         String qrCodeUrl = attendanceService.generateQrCode(code);

//         assertTrue(qrCodeUrl.contains(code), "URL phải chứa code ban đầu");
//         // Kiểm tra định dạng URL cơ bản
//         assertTrue(qrCodeUrl.startsWith("https://yourdomain/api/attendance/scan?code="), "URL phải đúng format");

//         // Không thể xác thực HMAC trực tiếp vì `hmacSha256` là private,
//         // nhưng ta có thể kiểm tra định dạng chữ ký (Base64 Url Safe)
//         String sig = qrCodeUrl.substring(qrCodeUrl.indexOf("&sig=") + 5);
//         assertFalse(sig.contains("+") || sig.contains("/"), "Signature phải là Base64 Url Safe");
//         assertFalse(sig.endsWith("="), "Signature phải không có padding");
//         assertFalse(sig.isEmpty(), "Signature không được rỗng");
//     }

//     // --- 2. Test processQrScan (Check-in) ---
//     @Test
//     void testProcessQrScan_firstScanShouldCheckIn() {
//         // Tạo QR và chữ ký hợp lệ
//         String code = "check-in-morning";
//         String qrCodeUrl = attendanceService.generateQrCode(code);
//         String sig = qrCodeUrl.substring(qrCodeUrl.indexOf("&sig=") + 5);

//         // Thực hiện check-in
//         String result = attendanceService.processQrScan(TEST_USER_ID, code, sig);

//         assertEquals("✅ Checked in successfully", result);

//         // 1. Kiểm tra Record đã được tạo trong DB
//         Optional<AttendanceRecord> recordOpt = recordRepo.findByInternIdAndWorkDate(TEST_INTERN_ID, LocalDate.now());
//         assertTrue(recordOpt.isPresent(), "AttendanceRecord phải được tạo");
//         AttendanceRecord record = recordOpt.get();
//         assertNotNull(record.getCheckInTime(), "CheckInTime phải được set");
//         assertNull(record.getCheckOutTime(), "CheckOutTime phải là null");
//         assertEquals("present", record.getStatus());

//         // 2. Kiểm tra Log đã được tạo
//         List<AttendanceLog> logs = logRepo.findAll();
//         assertEquals(1, logs.size(), "Phải có 1 log CHECKIN");
//         assertEquals(AttendanceLog.EventType.CHECKIN, logs.get(0).getEventType());
//     }

//     // --- 3. Test processQrScan (Check-out) ---
//     @Test
//     void testProcessQrScan_secondScanShouldCheckOut() throws InterruptedException {
//         // Setup: Check-in trước
//         String code = "check-in-morning";
//         String qrCodeUrl = attendanceService.generateQrCode(code);
//         String sig = qrCodeUrl.substring(qrCodeUrl.indexOf("&sig=") + 5);
//         attendanceService.processQrScan(TEST_USER_ID, code, sig);

//         // Đợi một chút để Check-outTime khác Check-inTime
//         Thread.sleep(100);

//         // Thực hiện check-out
//         String result = attendanceService.processQrScan(TEST_USER_ID, code, sig);

//         assertEquals("✅ Checked out successfully", result);

//         // 1. Kiểm tra Record trong DB: CheckOutTime phải được set
//         Optional<AttendanceRecord> recordOpt = recordRepo.findByInternIdAndWorkDate(TEST_INTERN_ID, LocalDate.now());
//         assertTrue(recordOpt.isPresent(), "AttendanceRecord phải tồn tại");
//         AttendanceRecord record = recordOpt.get();
//         assertNotNull(record.getCheckInTime(), "CheckInTime phải tồn tại");
//         assertNotNull(record.getCheckOutTime(), "CheckOutTime phải được set");
//         assertTrue(record.getCheckOutTime().isAfter(record.getCheckInTime()), "CheckOut phải sau CheckIn");

//         // 2. Kiểm tra Log
//         List<AttendanceLog> logs = logRepo.findAll();
//         assertEquals(2, logs.size(), "Phải có 2 log (CHECKIN và CHECKOUT)");
//         assertEquals(AttendanceLog.EventType.CHECKOUT, logs.get(1).getEventType());
//     }

//     // --- 4. Test processQrScan (Already checked out) ---
//     @Test
//     void testProcessQrScan_thirdScanShouldReturnAlreadyCheckedOut() {
//         // Setup: Check-in và Check-out trước
//         String code = "completed-day";
//         String qrCodeUrl = attendanceService.generateQrCode(code);
//         String sig = qrCodeUrl.substring(qrCodeUrl.indexOf("&sig=") + 5);
//         attendanceService.processQrScan(TEST_USER_ID, code, sig); // Check-in
//         attendanceService.processQrScan(TEST_USER_ID, code, sig); // Check-out

//         // Thực hiện scan lần 3
//         String result = attendanceService.processQrScan(TEST_USER_ID, code, sig);

//         assertEquals("⚠️ Already checked out today", result);

//         // Kiểm tra Log
//         List<AttendanceLog> logs = logRepo.findAll();
//         assertEquals(3, logs.size(), "Phải có 3 log (IN, OUT, EXPIRED)");
//         assertEquals(AttendanceLog.EventType.EXPIRED, logs.get(2).getEventType());
//     }

//     // --- 5. Test Invalid Signature ---
//     @Test
//     void testProcessQrScan_invalidSignatureShouldThrowExceptionAndLog() {
//         String code = "test-code";
//         String invalidSig = "wrong-signature";

//         // Thực hiện scan với chữ ký sai
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
//             attendanceService.processQrScan(TEST_USER_ID, code, invalidSig);
//         }, "Phải ném ra ngoại lệ Invalid QR signature");

//         assertEquals("Invalid QR signature!", thrown.getMessage());

//         // Kiểm tra Record: Không được tạo
//         Optional<AttendanceRecord> recordOpt = recordRepo.findByInternIdAndWorkDate(TEST_INTERN_ID, LocalDate.now());
//         assertFalse(recordOpt.isPresent(), "AttendanceRecord không được tạo");

//         // Kiểm tra Log: Phải có log ghi lại lỗi
//         List<AttendanceLog> logs = logRepo.findAll();
//         assertEquals(1, logs.size(), "Phải có 1 log cho chữ ký sai");
//         // Kiểm tra loại log, mặc dù trong code bạn đang set là CHECKIN,
//         // thực tế nên là một loại riêng như INVALID_SIG
//         assertEquals(AttendanceLog.EventType.CHECKIN, logs.get(0).getEventType());
//         assertEquals(invalidSig, logs.get(0).getSig());
//     }

//     // --- 6. Test Intern Context Error ---
//     @Test
//     void testProcessQrScan_userWithoutInternProfileShouldThrowException() {
//         Long NON_INTERN_USER_ID = 1L;
//         // Mock: Giả sử user này không có internId
//         when(internContextService.getInternIdFromUserId(NON_INTERN_USER_ID)).thenReturn(null);

//         String code = "valid-code";
//         String qrCodeUrl = attendanceService.generateQrCode(code);
//         String sig = qrCodeUrl.substring(qrCodeUrl.indexOf("&sig=") + 5);

//         // Thực hiện scan
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
//             attendanceService.processQrScan(NON_INTERN_USER_ID, code, sig);
//         }, "Phải ném ra ngoại lệ User không có hồ sơ thực tập sinh");

//         assertEquals("User này không có hồ sơ thực tập sinh!", thrown.getMessage());

//         // Kiểm tra Record và Log: Không được tạo
//         assertEquals(0, recordRepo.count());
//         assertEquals(0, logRepo.count());
//     }

//     // --- 7. Test getRecordsByDate ---
//     @Test
//     void testGetRecordsByDate_shouldReturnCorrectRecords() {
//         LocalDate yesterday = LocalDate.now().minusDays(1);

//         // Setup dữ liệu ngày hôm qua
//         AttendanceRecord recordYesterday = new AttendanceRecord();
//         recordYesterday.setIntern(testIntern);
//         recordYesterday.setWorkDate(yesterday);
//         recordYesterday.setCheckInTime(LocalDateTime.now().minusDays(1).withHour(9));
//         recordRepo.save(recordYesterday);

//         // Setup dữ liệu ngày hôm nay (từ test case khác)
//         AttendanceRecord recordToday = new AttendanceRecord();
//         recordToday.setIntern(testIntern);
//         recordToday.setWorkDate(LocalDate.now());
//         recordToday.setCheckInTime(LocalDateTime.now().withHour(9));
//         recordRepo.save(recordToday);

//         // Test
//         List<AttendanceRecord> records = attendanceService.getRecordsByDate(yesterday);

//         assertEquals(1, records.size(), "Chỉ nên trả về 1 record của ngày hôm qua");
//         assertEquals(yesterday, records.get(0).getWorkDate());
//     }

//     // --- 8. Test getRecordsByIntern ---
//     @Test
//     void testGetRecordsByIntern_shouldReturnAllRecordsForIntern() {
//         LocalDate yesterday = LocalDate.now().minusDays(1);
//         LocalDate today = LocalDate.now();

//         // Setup 2 Record cho intern này
//         AttendanceRecord recordYesterday = new AttendanceRecord();
//         recordYesterday.setIntern(testIntern);
//         recordYesterday.setWorkDate(yesterday);
//         recordYesterday.setCheckInTime(LocalDateTime.now().minusDays(1).withHour(9));
//         recordRepo.save(recordYesterday);

//         AttendanceRecord recordToday = new AttendanceRecord();
//         recordToday.setIntern(testIntern);
//         recordToday.setWorkDate(today);
//         recordToday.setCheckInTime(LocalDateTime.now().withHour(9));
//         recordRepo.save(recordToday);

//         // Test
//         List<AttendanceRecord> records = attendanceService.getRecordsByIntern(TEST_INTERN_ID);

//         assertEquals(2, records.size(), "Phải trả về 2 record cho intern này");
//         assertTrue(records.stream().anyMatch(r -> r.getWorkDate().equals(yesterday)));
//         assertTrue(records.stream().anyMatch(r -> r.getWorkDate().equals(today)));
//     }
// }