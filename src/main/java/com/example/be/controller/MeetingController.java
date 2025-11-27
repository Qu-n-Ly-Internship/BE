package com.example.be.controller;

import com.example.be.dto.meeting.*;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import com.example.be.service.JwtService;
import com.example.be.service.calendar.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MeetingController {

    private final MeetingService meetingService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * HR tạo meeting mới
     * POST /api/meetings
     */
    @PostMapping
    public ResponseEntity<?> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            User user = getUserFromToken(bearerToken);

            if (!hasHrRole(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Chỉ HR/Admin mới có quyền tạo meeting"));
            }

            // Validate time logic
            if (request.getEndTime().isBefore(request.getStartTime())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Thời gian kết thúc phải sau thời gian bắt đầu"));
            }

            MeetingResponse response = meetingService.createMeeting(request, user.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách meetings
     * GET /api/meetings
     */
    @GetMapping
    public ResponseEntity<List<MeetingListResponse>> getAllMeetings(
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            User user = getUserFromToken(bearerToken);
            List<MeetingListResponse> meetings = meetingService.getAllMeetings(
                    user.getId(),
                    user.getRole().getName()
            );
            return ResponseEntity.ok(meetings);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy chi tiết một meeting
     * GET /api/meetings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            User user = getUserFromToken(bearerToken);
            MeetingResponse meeting = meetingService.getMeetingById(id, user.getId());
            return ResponseEntity.ok(meeting);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cập nhật meeting
     * PUT /api/meetings/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMeetingRequest request,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            User user = getUserFromToken(bearerToken);

            if (!hasHrRole(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Chỉ HR/Admin mới có quyền cập nhật meeting"));
            }

            // Validate time if both provided
            if (request.getStartTime() != null && request.getEndTime() != null) {
                if (request.getEndTime().isBefore(request.getStartTime())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Thời gian kết thúc phải sau thời gian bắt đầu"));
                }
            }

            MeetingResponse response = meetingService.updateMeeting(id, request, user.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Xóa meeting
     * DELETE /api/meetings/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeeting(
            @PathVariable Long id,
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            User user = getUserFromToken(bearerToken);

            if (!hasHrRole(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Chỉ HR/Admin mới có quyền xóa meeting"));
            }

            meetingService.deleteMeeting(id, user.getId());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // ========== HELPER METHODS ==========

    private User getUserFromToken(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean hasHrRole(User user) {
        String role = user.getRole().getName();
        return "HR".equals(role) || "ADMIN".equals(role);
    }
}