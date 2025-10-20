package com.example.be.service;

import com.example.be.entity.AllowancePayment;
import com.example.be.entity.InternProfile;
import com.example.be.repository.AllowancePaymentRepository;
import com.example.be.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AllowanceService {
    private final AllowancePaymentRepository allowanceRepository;
    private final InternProfileRepository internProfileRepository;

    // Lấy danh sách phụ cấp với filter
    public Map<String, Object> getAllAllowances(String internId, String startDate, String endDate, int page, int size) {
        try {
            List<AllowancePayment> allowances;

            if (internId != null && !internId.isEmpty()) {
                Long iId = Long.parseLong(internId);
                if (startDate != null && endDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    allowances = allowanceRepository.findByInternIdAndDateRange(iId, start, end);
                } else {
                    allowances = allowanceRepository.findByIntern_Id(iId);
                }
            } else {
                allowances = allowanceRepository.findAll();
            }

            // Sort by date descending
            allowances.sort((a, b) -> b.getDate().compareTo(a.getDate()));

            // Pagination
            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, allowances.size());
            List<AllowancePayment> paged = allowances.subList(startIdx, endIdx);

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
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phụ cấp: " + e.getMessage());
        }
    }

    // Lấy chi tiết một phụ cấp
    public Map<String, Object> getAllowanceById(Long id) {
        try {
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
                            "note", allowance.getNote()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy chi tiết phụ cấp: " + e.getMessage());
        }
    }

    // Tạo phụ cấp mới
    public Map<String, Object> createAllowance(Map<String, Object> request) {
        try {
            Long internId = Long.parseLong(request.get("internId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            LocalDate date = LocalDate.parse(request.get("date").toString());
            String note = (String) request.getOrDefault("note", "");

            if (amount <= 0) {
                throw new IllegalArgumentException("Số tiền phụ cấp phải lớn hơn 0");
            }

            InternProfile intern = internProfileRepository.findById(internId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh với ID: " + internId));

            AllowancePayment allowance = AllowancePayment.builder()
                    .intern(intern)
                    .amount(amount)
                    .date(date)
                    .note(note)
                    .build();

            AllowancePayment saved = allowanceRepository.save(allowance);

            return Map.of(
                    "success", true,
                    "message", "Tạo phụ cấp thành công!",
                    "data", Map.of(
                            "allowanceId", saved.getId(),
                            "internName", saved.getIntern().getFullName(),
                            "amount", saved.getAmount(),
                            "date", saved.getDate()
                    )
            );
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

            AllowancePayment saved = allowanceRepository.save(allowance);

            return Map.of(
                    "success", true,
                    "message", "Cập nhật phụ cấp thành công!",
                    "data", Map.of(
                            "allowanceId", saved.getId(),
                            "amount", saved.getAmount(),
                            "date", saved.getDate()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật phụ cấp thất bại: " + e.getMessage());
        }
    }

    // Xóa phụ cấp
    public Map<String, Object> deleteAllowance(Long id) {
        try {
            if (!allowanceRepository.existsById(id)) {
                throw new RuntimeException("Không tìm thấy phụ cấp với ID: " + id);
            }
            allowanceRepository.deleteById(id);

            return Map.of(
                    "success", true,
                    "message", "Xóa phụ cấp thành công!"
            );
        } catch (Exception e) {
            throw new RuntimeException("Xóa phụ cấp thất bại: " + e.getMessage());
        }
    }

    // Duyệt/thanh toán phụ cấp
    public Map<String, Object> approveAllowance(Long id) {
        try {
            AllowancePayment allowance = allowanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ cấp với ID: " + id));

            if (allowance.getPaidAt() != null) {
                throw new IllegalStateException("Phụ cấp này đã được thanh toán rồi");
            }

            allowance.setPaidAt(LocalDateTime.now());
            AllowancePayment saved = allowanceRepository.save(allowance);

            return Map.of(
                    "success", true,
                    "message", "Phụ cấp đã được duyệt và thanh toán!",
                    "data", Map.of(
                            "allowanceId", saved.getId(),
                            "paidAt", saved.getPaidAt()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Duyệt phụ cấp thất bại: " + e.getMessage());
        }
    }

    // Lấy thống kê phụ cấp theo intern
    public Map<String, Object> getAllowanceStatsByIntern(Long internId) {
        try {
            List<AllowancePayment> allowances = allowanceRepository.findByIntern_Id(internId);
            Double total = allowanceRepository.getTotalAllowanceByInternId(internId);

            long paid = allowances.stream().filter(a -> a.getPaidAt() != null).count();
            long pending = allowances.stream().filter(a -> a.getPaidAt() == null).count();

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalAllowance", total,
                            "totalRecords", allowances.size(),
                            "paidCount", paid,
                            "pendingCount", pending,
                            "paidPercentage", allowances.size() > 0
                                    ? String.format("%.1f%%", (paid * 100.0 / allowances.size()))
                                    : "0%"
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // Lấy danh sách phụ cấp chờ duyệt
    public Map<String, Object> getPendingAllowances(int page, int size) {
        try {
            List<AllowancePayment> pending = allowanceRepository.findAll().stream()
                    .filter(a -> a.getPaidAt() == null)
                    .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                    .toList();

            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, pending.size());
            List<AllowancePayment> paged = pending.subList(startIdx, endIdx);

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : paged) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("date", a.getDate());
                map.put("createdAt", a.getCreatedAt());
                data.add(map);
            }

            return Map.of(
                    "success", true,
                    "data", data,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", pending.size(),
                            "totalPages", (int) Math.ceil((double) pending.size() / size)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách chờ duyệt: " + e.getMessage());
        }
    }

    // Lấy danh sách phụ cấp đã duyệt
    public Map<String, Object> getApprovedAllowances(int page, int size) {
        try {
            List<AllowancePayment> approved = allowanceRepository.findAll().stream()
                    .filter(a -> a.getPaidAt() != null)
                    .sorted((a, b) -> b.getPaidAt().compareTo(a.getPaidAt()))
                    .toList();

            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, approved.size());
            List<AllowancePayment> paged = approved.subList(startIdx, endIdx);

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : paged) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internId", a.getIntern().getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("date", a.getDate());
                map.put("paidAt", a.getPaidAt());
                data.add(map);
            }

            return Map.of(
                    "success", true,
                    "data", data,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", approved.size(),
                            "totalPages", (int) Math.ceil((double) approved.size() / size)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách đã duyệt: " + e.getMessage());
        }
    }

    // Thống kê tổng quát phụ cấp
    public Map<String, Object> getAllowanceDashboard() {
        try {
            List<AllowancePayment> all = allowanceRepository.findAll();

            double totalAmount = all.stream()
                    .mapToDouble(AllowancePayment::getAmount)
                    .sum();

            long paidCount = all.stream()
                    .filter(a -> a.getPaidAt() != null)
                    .count();

            long pendingCount = all.stream()
                    .filter(a -> a.getPaidAt() == null)
                    .count();

            double paidAmount = all.stream()
                    .filter(a -> a.getPaidAt() != null)
                    .mapToDouble(AllowancePayment::getAmount)
                    .sum();

            return Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalAllowances", all.size(),
                            "totalAmount", totalAmount,
                            "paidCount", paidCount,
                            "pendingCount", pendingCount,
                            "paidAmount", paidAmount,
                            "pendingAmount", totalAmount - paidAmount,
                            "approvalRate", all.size() > 0
                                    ? String.format("%.1f%%", (paidCount * 100.0 / all.size()))
                                    : "0%"
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // Duyệt nhiều phụ cấp cùng lúc
    public Map<String, Object> approveMultiple(List<Long> allowanceIds) {
        try {
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long id : allowanceIds) {
                try {
                    AllowancePayment allowance = allowanceRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("ID không tồn tại: " + id));

                    if (allowance.getPaidAt() != null) {
                        failCount++;
                        errors.add("Phụ cấp " + id + " đã được thanh toán");
                        continue;
                    }

                    allowance.setPaidAt(LocalDateTime.now());
                    allowanceRepository.save(allowance);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("Lỗi phụ cấp " + id + ": " + e.getMessage());
                }
            }

            return Map.of(
                    "success", failCount == 0,
                    "message", "Duyệt " + successCount + "/" + allowanceIds.size() + " phụ cấp",
                    "successCount", successCount,
                    "failCount", failCount,
                    "errors", errors
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi duyệt nhiều: " + e.getMessage());
        }
    }

    // Xuất báo cáo phụ cấp theo tháng
    public Map<String, Object> getMonthlyReport(String month) {
        try {
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);

            LocalDate startDate = LocalDate.of(year, monthNum, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);

            List<AllowancePayment> monthlyData = allowanceRepository.findAll().stream()
                    .filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
                    .sorted((a, b) -> a.getIntern().getFullName().compareTo(b.getIntern().getFullName()))
                    .toList();

            Map<String, Object> summary = new HashMap<>();
            summary.put("month", month);
            summary.put("totalRecords", monthlyData.size());
            summary.put("totalAmount", monthlyData.stream().mapToDouble(AllowancePayment::getAmount).sum());
            summary.put("approvedCount", monthlyData.stream().filter(a -> a.getPaidAt() != null).count());
            summary.put("pendingCount", monthlyData.stream().filter(a -> a.getPaidAt() == null).count());

            List<Map<String, Object>> details = new ArrayList<>();
            for (AllowancePayment a : monthlyData) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internId", a.getIntern().getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("status", a.getPaidAt() != null ? "ĐÃ DUYỆT" : "CHỜ DUYỆT");
                map.put("paidAt", a.getPaidAt());
                details.add(map);
            }

            return Map.of(
                    "success", true,
                    "summary", summary,
                    "details", details
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo: " + e.getMessage());
        }
    }


    // Tìm kiếm phụ cấp
    public Map<String, Object> searchAllowances(String keyword, int page, int size) {
        try {
            List<AllowancePayment> all = allowanceRepository.findAll();
            String lowerKeyword = keyword.toLowerCase();

            List<AllowancePayment> filtered = all.stream()
                    .filter(a -> a.getIntern().getFullName().toLowerCase().contains(lowerKeyword) ||
                            a.getNote().toLowerCase().contains(lowerKeyword) ||
                            String.valueOf(a.getAmount()).contains(keyword))
                    .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                    .toList();

            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, filtered.size());
            List<AllowancePayment> paged = filtered.subList(startIdx, endIdx);

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : paged) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("internId", a.getIntern().getId());
                map.put("internName", a.getIntern().getFullName());
                map.put("amount", a.getAmount());
                map.put("date", a.getDate());
                map.put("paidAt", a.getPaidAt());
                map.put("note", a.getNote());
                data.add(map);
            }

            return Map.of(
                    "success", true,
                    "data", data,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", filtered.size(),
                            "totalPages", (int) Math.ceil((double) filtered.size() / size)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm: " + e.getMessage());
        }
    }

    // Xuất Excel báo cáo phụ cấp
    public Map<String, Object> exportAllowancesReport() {
        try {
            List<AllowancePayment> all = allowanceRepository.findAll();

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (AllowancePayment a : all) {
                Map<String, Object> map = new HashMap<>();
                map.put("STT", reportData.size() + 1);
                map.put("Mã phụ cấp", a.getId());
                map.put("Tên thực tập sinh", a.getIntern().getFullName());
                map.put("Số tiền", String.format("%.0f VNĐ", a.getAmount()));
                map.put("Ngày cấp phát", a.getDate());
                map.put("Trạng thái", a.getPaidAt() != null ? "ĐÃ DUYỆT" : "CHỜ DUYỆT");
                map.put("Ngày duyệt", a.getPaidAt());
                map.put("Ghi chú", a.getNote());
                reportData.add(map);
            }

            return Map.of(
                    "success", true,
                    "message", "Xuất báo cáo thành công",
                    "totalRecords", all.size(),
                    "totalAmount", String.format("%.0f VNĐ", all.stream().mapToDouble(AllowancePayment::getAmount).sum()),
                    "data", reportData
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất báo cáo: " + e.getMessage());
        }
    }

    // Thống kê phụ cấp theo từng intern
    public Map<String, Object> getAllowanceByAllInterns() {
        try {
            List<AllowancePayment> all = allowanceRepository.findAll();

            Map<Long, Map<String, Object>> internStats = new HashMap<>();
            for (AllowancePayment a : all) {
                Long internId = a.getIntern().getId();
                internStats.putIfAbsent(internId, new HashMap<>());

                Map<String, Object> stats = internStats.get(internId);
                stats.put("internId", internId);
                stats.put("internName", a.getIntern().getFullName());

                Double currentTotal = (Double) stats.getOrDefault("totalAmount", 0.0);
                stats.put("totalAmount", currentTotal + a.getAmount());

                Long paidCount = (Long) stats.getOrDefault("paidCount", 0L);
                if (a.getPaidAt() != null) {
                    stats.put("paidCount", paidCount + 1);
                }

                Long pendingCount = (Long) stats.getOrDefault("pendingCount", 0L);
                if (a.getPaidAt() == null) {
                    stats.put("pendingCount", pendingCount + 1);
                }

                Long totalCount = (Long) stats.getOrDefault("totalCount", 0L);
                stats.put("totalCount", totalCount + 1);
            }

            List<Map<String, Object>> result = new ArrayList<>(internStats.values());
            result.sort((a, b) -> ((Double) b.get("totalAmount")).compareTo((Double) a.get("totalAmount")));

            return Map.of(
                    "success", true,
                    "totalInterns", internStats.size(),
                    "data", result
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // Lấy lịch sử thay đổi phụ cấp
    public Map<String, Object> getAllowanceHistory(Long internId) {
        try {
            List<AllowancePayment> history = allowanceRepository.findByIntern_Id(internId);
            history.sort((a, b) -> b.getDate().compareTo(a.getDate()));

            List<Map<String, Object>> data = new ArrayList<>();
            for (AllowancePayment a : history) {
                Map<String, Object> map = new HashMap<>();
                map.put("allowanceId", a.getId());
                map.put("amount", a.getAmount());
                map.put("date", a.getDate());
                map.put("createdAt", a.getCreatedAt());
                map.put("status", a.getPaidAt() != null ? "ĐÃ DUYỆT" : "CHỜ DUYỆT");
                map.put("paidAt", a.getPaidAt());
                map.put("note", a.getNote());
                data.add(map);
            }

            Double totalAmount = allowanceRepository.getTotalAllowanceByInternId(internId);

            return Map.of(
                    "success", true,
                    "internId", internId,
                    "totalAmount", totalAmount,
                    "totalRecords", data.size(),
                    "history", data
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy lịch sử: " + e.getMessage());
        }
    }

}