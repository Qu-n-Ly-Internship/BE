package com.example.be.service;

import com.example.be.dto.*;
import com.example.be.entity.LeaveRequest;
import com.example.be.entity.InternProfile;
import com.example.be.entity.User;
import com.example.be.repository.LeaveRequestRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ==================== INTERN APIs ====================

    /**
     * Intern tạo đơn nghỉ phép
     */
    @Transactional
    public LeaveRequestResponse createLeaveRequest(LeaveRequestDTO dto) {
        // Validate dates
        validateDates(dto.getStartDate(), dto.getEndDate());

        // Tìm intern
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + dto.getEmail()));

        InternProfile intern = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thực tập sinh"));

        // Tạo đơn nghỉ phép
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .intern(intern)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status("PENDING")
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.fromEntity(saved);
    }

    /**
     * Intern xem lịch sử đơn nghỉ phép
     */
    public List<LeaveRequestResponse> getMyLeaveRequests(String email) {
        // Bước 1: Tìm User theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

        // Bước 2: Tìm InternProfile của user này
        InternProfile intern = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thực tập sinh"));

        // Bước 3: CRITICAL - Chỉ lấy đơn của intern này
        List<LeaveRequest> requests = leaveRequestRepository.findByIntern_Id(intern.getId());

        // DEBUG: In ra để kiểm tra
        System.out.println("=== DEBUG getMyLeaveRequests ===");
        System.out.println("Email: " + email);
        System.out.println("User ID: " + user.getId());
        System.out.println("Intern ID: " + intern.getId());
        System.out.println("Found " + requests.size() + " leave requests");
        System.out.println("================================");

        return requests.stream()
                .map(req -> {
                    LeaveRequestResponse response = LeaveRequestResponse.fromEntity(req);

                    // Thêm tên người duyệt nếu có
                    if (req.getReviewedBy() != null) {
                        userRepository.findById(req.getReviewedBy()).ifPresent(reviewer ->
                                response.setReviewerName(reviewer.getFullName())
                        );
                    }

                    return response;
                })
                .sorted(Comparator.comparing(LeaveRequestResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    // ==================== HR APIs ====================

    /**
     * HR xem tất cả đơn nghỉ phép (có filter + pagination)
     */
    public Map<String, Object> getAllLeaveRequests(String status, String internName, int page, int size) {
        List<LeaveRequest> allRequests;

        if (status != null && !status.trim().isEmpty()) {
            allRequests = leaveRequestRepository.findByStatus(status);
        } else {
            allRequests = leaveRequestRepository.findAll();
        }

        // Filter theo tên intern
        if (internName != null && !internName.trim().isEmpty()) {
            String searchTerm = internName.toLowerCase();
            allRequests = allRequests.stream()
                    .filter(req -> req.getIntern().getFullName().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
        }

        // Sort theo ngày tạo mới nhất
        allRequests.sort(Comparator.comparing(LeaveRequest::getCreatedAt).reversed());

        // Pagination
        int start = page * size;
        int end = Math.min(start + size, allRequests.size());
        List<LeaveRequest> pagedRequests = start < allRequests.size()
                ? allRequests.subList(start, end)
                : new ArrayList<>();

        List<LeaveRequestResponse> data = pagedRequests.stream()
                .map(req -> {
                    LeaveRequestResponse response = LeaveRequestResponse.fromEntity(req);

                    // Thêm tên người duyệt
                    if (req.getReviewedBy() != null) {
                        userRepository.findById(req.getReviewedBy()).ifPresent(reviewer ->
                                response.setReviewerName(reviewer.getFullName())
                        );
                    }

                    return response;
                })
                .collect(Collectors.toList());

        return Map.of(
                "success", true,
                "data", data,
                "pagination", Map.of(
                        "currentPage", page,
                        "pageSize", size,
                        "totalElements", allRequests.size(),
                        "totalPages", (int) Math.ceil((double) allRequests.size() / size)
                )
        );
    }

    /**
     * HR xem đơn chờ duyệt
     */
    public List<LeaveRequestResponse> getPendingRequests() {
        return leaveRequestRepository.findPendingRequests()
                .stream()
                .map(LeaveRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * HR duyệt đơn nghỉ phép
     */
    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long id, LeaveReviewRequest reviewRequest) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Đơn nghỉ phép đã được xử lý rồi!");
        }

        User hr = userRepository.findByEmail(reviewRequest.getHrEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR"));

        request.setStatus("APPROVED");
        request.setReviewedBy(hr.getId());
        request.setReviewedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(request);

        // Gửi email thông báo
        sendApprovalEmail(saved);

        LeaveRequestResponse response = LeaveRequestResponse.fromEntity(saved);
        response.setReviewerName(hr.getFullName());

        return response;
    }

    /**
     * HR từ chối đơn nghỉ phép
     */
    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long id, LeaveReviewRequest reviewRequest) {
        if (reviewRequest.getRejectionReason() == null || reviewRequest.getRejectionReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }

        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ phép"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Đơn nghỉ phép đã được xử lý rồi!");
        }

        User hr = userRepository.findByEmail(reviewRequest.getHrEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR"));

        request.setStatus("REJECTED");
        request.setReviewedBy(hr.getId());
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(reviewRequest.getRejectionReason());

        LeaveRequest saved = leaveRequestRepository.save(request);

        // Gửi email thông báo
        sendRejectionEmail(saved);

        LeaveRequestResponse response = LeaveRequestResponse.fromEntity(saved);
        response.setReviewerName(hr.getFullName());

        return response;
    }

    /**
     * HR xem thống kê
     */
    public LeaveRequestStatsResponse getLeaveRequestStats() {
        return LeaveRequestStatsResponse.builder()
                .total(leaveRequestRepository.count())
                .pending(leaveRequestRepository.countByStatus("PENDING"))
                .approved(leaveRequestRepository.countByStatus("APPROVED"))
                .rejected(leaveRequestRepository.countByStatus("REJECTED"))
                .build();
    }

    // ==================== HELPER METHODS ====================

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể là ngày trong quá khứ");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void sendApprovalEmail(LeaveRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("name", request.getIntern().getFullName());
            params.put("startDate", request.getStartDate().toString());
            params.put("endDate", request.getEndDate().toString());
            params.put("leaveDays", String.valueOf(request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1));

            emailService.sendEmailFromTemplate(
                    request.getIntern().getEmail(),
                    "LEAVE_APPROVAL",
                    params
            );
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }

    private void sendRejectionEmail(LeaveRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("name", request.getIntern().getFullName());
            params.put("startDate", request.getStartDate().toString());
            params.put("endDate", request.getEndDate().toString());
            params.put("reason", request.getRejectionReason());

            emailService.sendEmailFromTemplate(
                    request.getIntern().getEmail(),
                    "LEAVE_REJECTION",
                    params
            );
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }
}