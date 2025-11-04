package com.example.be.controller;

import com.example.be.dto.*;
import com.example.be.service.LeaveRequestService;
import com.example.be.service.JwtService;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // ==================== INTERN APIs ====================


    @PostMapping("")
    public ResponseEntity<?> createLeaveRequest(@RequestBody LeaveRequestDTO dto) {
        try {
            LeaveRequestResponse response = leaveRequestService.createLeaveRequest(dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gửi đơn nghỉ phép thành công! Chờ HR phê duyệt.",
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo đơn nghỉ phép: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyLeaveRequests(@RequestParam String email) {
        try {
            List<LeaveRequestResponse> data = leaveRequestService.getMyLeaveRequests(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", data,
                    "total", data.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }


    @GetMapping("/me")
    public ResponseEntity<?> getMyLeaveRequestsByToken(
            @RequestHeader("Authorization") String bearerToken) {
        try {
            String token = bearerToken.replace("Bearer ", "");
            String email = jwtService.extractUsername(token);

            List<LeaveRequestResponse> data = leaveRequestService.getMyLeaveRequests(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", data,
                    "total", data.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token không hợp lệ: " + e.getMessage()
            ));
        }
    }

    // ==================== HR APIs ====================


    @GetMapping("")
    public ResponseEntity<?> getAllLeaveRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String internName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = leaveRequestService.getAllLeaveRequests(status, internName, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * HR xem danh sách đơn chờ duyệt
     * GET /api/leave-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
            List<LeaveRequestResponse> data = leaveRequestService.getPendingRequests();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", data,
                    "total", data.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * HR duyệt đơn nghỉ phép
     * PUT /api/leave-requests/{id}/approve
     * Body: { "hrEmail": "hr@example.com" }
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveReviewRequest request) {
        try {
            if (request.getHrEmail() == null || request.getHrEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu thông tin HR"
                ));
            }

            LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Duyệt đơn nghỉ phép thành công!",
                    "data", response
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi duyệt đơn: " + e.getMessage()
            ));
        }
    }

    /**
     * HR duyệt đơn nghỉ phép (dùng token)
     * PUT /api/leave-requests/{id}/approve-by-token
     * Header: Authorization: Bearer <token>
     */
    @PutMapping("/{id}/approve-by-token")
    public ResponseEntity<?> approveLeaveRequestByToken(
            @PathVariable Long id,
            @RequestHeader("Authorization") String bearerToken) {
        try {
            String token = bearerToken.replace("Bearer ", "");
            String email = jwtService.extractUsername(token);

            // Verify HR role
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            String roleName = user.getRole().getName();
            if (!"HR".equals(roleName) && !"ADMIN".equals(roleName)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Bạn không có quyền duyệt đơn nghỉ phép!"
                ));
            }

            LeaveReviewRequest request = LeaveReviewRequest.builder()
                    .hrEmail(email)
                    .build();

            LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Duyệt đơn nghỉ phép thành công!",
                    "data", response
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * HR từ chối đơn nghỉ phép
     * PUT /api/leave-requests/{id}/reject
     * Body: { "hrEmail": "hr@example.com", "rejectionReason": "Lý do từ chối" }
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveReviewRequest request) {
        try {
            if (request.getHrEmail() == null || request.getHrEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Thiếu thông tin HR"
                ));
            }

            LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Từ chối đơn nghỉ phép thành công!",
                    "data", response
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi từ chối đơn: " + e.getMessage()
            ));
        }
    }

    /**
     * HR từ chối đơn nghỉ phép (dùng token)
     * PUT /api/leave-requests/{id}/reject-by-token
     * Header: Authorization: Bearer <token>
     * Body: { "rejectionReason": "Lý do từ chối" }
     */
    @PutMapping("/{id}/reject-by-token")
    public ResponseEntity<?> rejectLeaveRequestByToken(
            @PathVariable Long id,
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody Map<String, String> body) {
        try {
            String token = bearerToken.replace("Bearer ", "");
            String email = jwtService.extractUsername(token);

            // Verify HR role
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            String roleName = user.getRole().getName();
            if (!"HR".equals(roleName) && !"ADMIN".equals(roleName)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Bạn không có quyền từ chối đơn nghỉ phép!"
                ));
            }

            LeaveReviewRequest request = LeaveReviewRequest.builder()
                    .hrEmail(email)
                    .rejectionReason(body.get("rejectionReason"))
                    .build();

            LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Từ chối đơn nghỉ phép thành công!",
                    "data", response
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * HR xem thống kê đơn nghỉ phép
     * GET /api/leave-requests/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getLeaveRequestStats() {
        try {
            LeaveRequestStatsResponse stats = leaveRequestService.getLeaveRequestStats();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
}