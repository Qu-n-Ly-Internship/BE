package com.example.be.service.calendar;

import com.example.be.dto.meeting.*;
import com.example.be.entity.*;
import com.example.be.notification.service.NotificationPublisher;
import com.example.be.repository.*;
import com.example.be.service.HrContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final InternProfileRepository internProfileRepository;
    private final HrRepository hrRepository;
    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    private final NotificationPublisher notificationPublisher;
    private final HrContextService hrContextService;

    /**
     * HR tạo meeting mới và đồng bộ lên Google Calendar
     */
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, Long hrUserId) {

        // Get HR
        Long hrId = hrContextService.getHrIdFromUserId(hrUserId);
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR"));

        // Get Intern
        InternProfile intern = internProfileRepository.findById(request.getInternId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh với ID: " + request.getInternId()));

        // Validate intern has Google auth
        User internUser = intern.getUser();
        if (internUser == null || !"GOOGLE".equals(internUser.getAuthProvider())) {
            throw new RuntimeException("Thực tập sinh này không đăng nhập bằng Google nên không thể đồng bộ lịch");
        }

        // Create meeting in database
        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .intern(intern)
                .createdBy(hr)
                .status("SCHEDULED")
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);

        // Sync to Google Calendar (if token available)
        String googleEventId = null;
        try {
            // TODO: Get real access token
            // String accessToken = getInternAccessToken(internUser.getId());

            // googleEventId = googleCalendarService.createEvent(
            //     accessToken,
            //     request.getTitle(),
            //     request.getDescription(),
            //     request.getStartTime(),
            //     request.getEndTime(),
            //     request.getLocation(),
            //     internUser.getEmail()
            // );

            // savedMeeting.setGoogleEventId(googleEventId);
            // meetingRepository.save(savedMeeting);

            System.out.println("⚠️ Google Calendar sync is disabled - need OAuth token");

        } catch (Exception e) {
            System.err.println("❌ Failed to sync with Google Calendar: " + e.getMessage());
            // Continue anyway
        }

        // Send notification
        try {
            String dateStr = request.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            notificationPublisher.publish(
                    internUser.getId().toString(),
                    "NEW_MEETING",
                    "📅 Lịch họp mới từ " + hr.getFullname(),
                    String.format("%s\n🕐 %s\n📍 %s",
                            request.getTitle(),
                            dateStr,
                            request.getLocation() != null ? request.getLocation() : "Chưa xác định"
                    )
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to send notification: " + e.getMessage());
        }

        return mapToResponse(savedMeeting);
    }

    /**
     * Lấy danh sách meetings
     */
    public List<MeetingListResponse> getAllMeetings(Long userId, String role) {
        List<Meeting> meetings;

        if ("HR".equals(role) || "ADMIN".equals(role)) {
            Long hrId = hrContextService.getHrIdFromUserId(userId);
            meetings = meetingRepository.findByCreatedBy_Id(hrId);
        } else {
            InternProfile intern = internProfileRepository.findByUser_Id(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thực tập sinh"));
            meetings = meetingRepository.findByIntern_Id(intern.getId());
        }

        return meetings.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một meeting
     */
    public MeetingResponse getMeetingById(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy meeting"));

        // Check permission (HR created it OR intern is participant)
        // Add your permission check here

        return mapToResponse(meeting);
    }

    /**
     * Cập nhật meeting
     */
    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long hrUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy meeting"));

        // Check permission
        Long hrId = hrContextService.getHrIdFromUserId(hrUserId);
        if (!meeting.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa meeting này");
        }

        // Update fields (only if provided)
        if (request.getTitle() != null) {
            meeting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            meeting.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            meeting.setLocation(request.getLocation());
        }
        if (request.getStartTime() != null) {
            meeting.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            meeting.setEndTime(request.getEndTime());
        }

        Meeting updated = meetingRepository.save(meeting);

        // TODO: Update Google Calendar event if googleEventId exists

        return mapToResponse(updated);
    }

    /**
     * Xóa meeting
     */
    @Transactional
    public void deleteMeeting(Long meetingId, Long hrUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy meeting"));

        // Check permission
        Long hrId = hrContextService.getHrIdFromUserId(hrUserId);
        if (!meeting.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("Bạn không có quyền xóa meeting này");
        }

        // TODO: Delete from Google Calendar if googleEventId exists

        meetingRepository.delete(meeting);
    }

    // ========== MAPPER METHODS ==========

    private MeetingResponse mapToResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .googleEventId(meeting.getGoogleEventId())
                .internId(meeting.getIntern().getId())
                .internName(meeting.getIntern().getFullName())
                .internEmail(meeting.getIntern().getUser().getEmail())
                .hrId(meeting.getCreatedBy().getId())
                .hrName(meeting.getCreatedBy().getFullname())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    private MeetingListResponse mapToListResponse(Meeting meeting) {
        return MeetingListResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .internName(meeting.getIntern().getFullName())
                .createdBy(meeting.getCreatedBy().getFullname())
                .build();
    }
}