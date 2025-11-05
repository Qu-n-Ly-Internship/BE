package com.example.be.service;

import com.example.be.entity.AllowancePayment;
import com.example.be.entity.InternProfile;
import com.example.be.repository.AllowancePaymentRepository;
import com.example.be.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import com.example.be.service.JwtService;

@Service
@RequiredArgsConstructor
public class AllowanceService {
    private final AllowancePaymentRepository allowanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // Lấy danh sách phụ cấp với filter
    public Map<String, Object> getAllAllowances(String internId, String startDate, String endDate, int page, int size) {
        try {
            System.out.println("=== getAllAllowances called ===");
            List<AllowancePayment> allowances;

            if (internId != null && !internId.trim().isEmpty()) {
                try {
                    Long iId = Long.parseLong(internId.trim());
                    if (startDate != null && !startDate.trim().isEmpty() &&
                            endDate != null && !endDate.trim().isEmpty()) {
                        LocalDate start = LocalDate.parse(startDate);
                        LocalDate end = LocalDate.parse(endDate);
                        allowances = allowanceRepository.findByInternIdAndDateRange(iId, start, end);
                    } else {
                        allowances = allowanceRepository.findByIntern_Id(iId);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Intern ID không hợp lệ: " + internId);
                }
            } else {
                System.out.println("Fetching all allowances from database...");
                allowances = allowanceRepository.findAll();
                System.out.println("Found " + allowances.size() + " allowances");
            }

            if (allowances == null) {
                allowances = new ArrayList<>();
            }

            // Sort by date descending
            allowances.sort((a, b) -> b.getDate().compareTo(a.getDate()));

            // Pagination
            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, allowances.size());
            List<AllowancePayment> paged = startIdx < allowances.size()
                    ? allowances.subList(startIdx, endIdx)
                    : new ArrayList<>();

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : paged) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internId", a.getIntern().getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("date", a.getDate());
                map.put("paidAt", a.getPaidAt());
                map.put("note", a.getNote() != null ? a.getNote() : "");
                // ✅ FIX: Change "allowType" to "allowanceType"
                map.put("allowanceType", a.getAllowanceType() != null ? a.getAllowanceType() : "");
                data.add(map);
            }

            return Map.of(
                    "success", true,
                    "data", data,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", allowances.size(),
                            "totalPages", (int) Math.ceil((double) allowances.size() / size)
                    )
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy danh sách phụ cấp: " + e.getMessage(), e);
        }
    }

    // Lấy chi tiết một phụ cấp
    public Map<String, Object> getAllowanceById(Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("ID không hợp lệ");
            }

            AllowancePayment allowance = allowanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ cấp với ID: " + id));

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "allowanceId", allowance.getId(),
                            "internId", allowance.getIntern().getId(),
                            "internName", allowance.getIntern().getFullName(),
                            "amount", allowance.getAmount(),
                            "date", allowance.getDate(),
                            "createdAt", allowance.getCreatedAt(),
                            "paidAt", allowance.getPaidAt(),
                            "note", allowance.getNote() != null ? allowance.getNote() : "",
                            // ✅ FIX: Change "allowType" to "allowanceType"
                            "allowanceType", allowance.getAllowanceType() != null ? allowance.getAllowanceType() : ""
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy chi tiết phụ cấp: " + e.getMessage());
        }
    }

    // ✅ Tạo phụ cấp mới - TỰ ĐỘNG DUYỆT NGAY
    public Map<String, Object> createAllowance(Map<String, Object> request) {
        try {
            // Validate input
            if (request.get("internId") == null) {
                throw new IllegalArgumentException("Vui lòng chọn thực tập sinh");
            }
            if (request.get("amount") == null) {
                throw new IllegalArgumentException("Vui lòng nhập số tiền");
            }
            if (request.get("date") == null) {
                throw new IllegalArgumentException("Vui lòng chọn ngày");
            }

            Long internId = Long.parseLong(request.get("internId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            LocalDate date = LocalDate.parse(request.get("date").toString());
            String note = (String) request.getOrDefault("note", "");
            // ✅ FIX: Change "allowType" to "allowanceType" to match frontend
            String allowanceType = (String) request.getOrDefault("allowanceType", "");

            if (amount <= 0) {
                throw new IllegalArgumentException("Số tiền phụ cấp phải lớn hơn 0");
            }

            InternProfile intern = internProfileRepository.findById(internId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh với ID: " + internId));

            // ✅ TỰ ĐỘNG SET paidAt = hiện tại (đã duyệt ngay)
            AllowancePayment allowance = AllowancePayment.builder()
                    .intern(intern)
                    .amount(amount)
                    .date(date)
                    .note(note)
                    .allowanceType(allowanceType)
                    .paidAt(LocalDateTime.now()) // ✅ TỰ ĐỘNG DUYỆT
                    .build();

            AllowancePayment saved = allowanceRepository.save(allowance);

            return Map.of(
                    "success", true,
                    "message", "Thêm phụ cấp thành công!",
                    "data", Map.of(
                            "allowanceId", saved.getId(),
                            "internName", saved.getIntern().getFullName(),
                            "amount", saved.getAmount(),
                            "date", saved.getDate(),
                            "paidAt", saved.getPaidAt(),
                            "allowanceType", saved.getAllowanceType() != null ? saved.getAllowanceType() : ""
                    )
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Định dạng số không hợp lệ");
        } catch (Exception e) {
            throw new RuntimeException("Tạo phụ cấp thất bại: " + e.getMessage());
        }
    }

    // Cập nhật phụ cấp
    public Map<String, Object> updateAllowance(Long id, Map<String, Object> request) {
        try {
            AllowancePayment allowance = allowanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ cấp với ID: " + id));

            if (request.containsKey("amount")) {
                Double amount = Double.parseDouble(request.get("amount").toString());
                if (amount <= 0) {
                    throw new IllegalArgumentException("Số tiền phụ cấp phải lớn hơn 0");
                }
                allowance.setAmount(amount);
            }

            if (request.containsKey("date")) {
                allowance.setDate(LocalDate.parse(request.get("date").toString()));
            }

            if (request.containsKey("note")) {
                allowance.setNote((String) request.get("note"));
            }

            // ✅ FIX: Change "allowType" to "allowanceType"
            if (request.containsKey("allowanceType")) {
                allowance.setAllowanceType((String) request.get("allowanceType"));
            }

            AllowancePayment saved = allowanceRepository.save(allowance);

            return Map.of(
                    "success", true,
                    "message", "Cập nhật phụ cấp thành công!",
                    "data", Map.of(
                            "allowanceId", saved.getId(),
                            "amount", saved.getAmount(),
                            "date", saved.getDate(),
                            "allowanceType", saved.getAllowanceType() != null ? saved.getAllowanceType() : ""
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật phụ cấp thất bại: " + e.getMessage());
        }
    }

    // Xóa phụ cấp
    public Map<String, Object> deleteAllowance(Long id) {
        try {
            AllowancePayment allowance = allowanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ cấp với ID: " + id));

            allowanceRepository.deleteById(id);

            return Map.of(
                    "success", true,
                    "message", "Xóa phụ cấp thành công!"
            );
        } catch (Exception e) {
            throw new RuntimeException("Xóa phụ cấp thất bại: " + e.getMessage());
        }
    }

    // ⚠️ GIỮ LẠI để tránh lỗi nếu frontend vẫn gọi
    @Deprecated
    public Map<String, Object> approveAllowance(Long id) {
        return Map.of(
                "success", false,
                "message", "Chức năng duyệt đã bị vô hiệu hóa. Phụ cấp tự động được duyệt khi tạo."
        );
    }

    // Lấy thống kê phụ cấp theo intern
    public Map<String, Object> getAllowanceStatsByIntern(Long internId) {
        try {
            List<AllowancePayment> allowances = allowanceRepository.findByIntern_Id(internId);
            if (allowances == null) {
                allowances = new ArrayList<>();
            }

            Double total = allowanceRepository.getTotalAllowanceByInternId(internId);
            if (total == null) {
                total = 0.0;
            }

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalAllowance", total,
                            "totalRecords", allowances.size()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // ⚠️ BỎ các endpoint pending/approved vì không còn ý nghĩa
    @Deprecated
    public Map<String, Object> getPendingAllowances(int page, int size) {
        return Map.of("success", true, "data", new ArrayList<>(), "pagination", Map.of());
    }

    @Deprecated
    public Map<String, Object> getApprovedAllowances(int page, int size) {
        return getAllAllowances(null, null, null, page, size);
    }

    // Thống kê tổng quát phụ cấp
    public Map<String, Object> getAllowanceDashboard() {
        try {
            List<AllowancePayment> all = allowanceRepository.findAll();
            if (all == null) {
                all = new ArrayList<>();
            }

            double totalAmount = all.stream()
                    .mapToDouble(AllowancePayment::getAmount)
                    .sum();

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalAllowances", all.size(),
                            "totalAmount", totalAmount
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // ⚠️ BỎ duyệt nhiều
    @Deprecated
    public Map<String, Object> approveMultiple(List<Long> allowanceIds) {
        return Map.of(
                "success", false,
                "message", "Chức năng duyệt nhiều đã bị vô hiệu hóa."
        );
    }

    // Xuất báo cáo phụ cấp theo tháng
    public Map<String, Object> getMonthlyReport(String month) {
        try {
            if (month == null || month.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn tháng");
            }

            String[] parts = month.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Định dạng tháng không hợp lệ. Sử dụng: YYYY-MM");
            }

            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);

            if (monthNum < 1 || monthNum > 12) {
                throw new IllegalArgumentException("Tháng phải từ 1-12");
            }

            LocalDate startDate = LocalDate.of(year, monthNum, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);

            List<AllowancePayment> allAllowances = allowanceRepository.findAll();
            List<AllowancePayment> monthlyData = allAllowances.stream()
                    .filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
                    .sorted((a, b) -> a.getIntern().getFullName().compareTo(b.getIntern().getFullName()))
                    .toList();

            Map<String, Object> summary = new HashMap<>();
            summary.put("month", month);
            summary.put("totalRecords", monthlyData.size());
            summary.put("totalAmount", monthlyData.stream().mapToDouble(AllowancePayment::getAmount).sum());

            List<Map<String, Object>> details = new ArrayList<>();
            for (AllowancePayment a : monthlyData) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internId", a.getIntern().getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("paidAt", a.getPaidAt());
                map.put("allowanceType", a.getAllowanceType() != null ? a.getAllowanceType() : "");
                details.add(map);
            }

            return Map.of(
                    "success", true,
                    "summary", summary,
                    "details", details
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Định dạng tháng không hợp lệ");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo: " + e.getMessage());
        }
    }

    // Lấy lịch sử phụ cấp của thực tập sinh hiện tại
    public Map<String, Object> getMyAllowanceHistory(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

            InternProfile intern = internProfileRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Bạn không phải là thực tập sinh"));

            List<AllowancePayment> allowances = allowanceRepository.findByIntern_Id(intern.getId());

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : allowances) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("allowanceType", a.getAllowanceType() != null ? a.getAllowanceType() : "");
                map.put("amount", a.getAmount());
                map.put("applyDate", a.getDate());
                map.put("paidAt", a.getPaidAt());
                map.put("note", a.getNote() != null ? a.getNote() : "");
                data.add(map);
            }

            data.sort((a, b) -> {
                LocalDate dateA = (LocalDate) a.get("applyDate");
                LocalDate dateB = (LocalDate) b.get("applyDate");
                return dateB.compareTo(dateA);
            });

            Double totalReceived = allowances.stream()
                    .mapToDouble(AllowancePayment::getAmount)
                    .sum();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalReceived", totalReceived);
            summary.put("totalRecords", data.size());
            summary.put("internName", intern.getFullName());

            return Map.of(
                    "success", true,
                    "data", data,
                    "summary", summary
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy lịch sử phụ cấp: " + e.getMessage());
        }
    }
}