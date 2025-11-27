package com.example.be.service.calendar;

import com.example.be.dto.meeting.*;
import com.example.be.entity.*;
import com.example.be.notification.service.NotificationPublisher;
import com.example.be.repository.*;
import com.example.be.service.HrContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final InternProfileRepository internProfileRepository;
    private final ProjectRepository projectRepository;
    private final HrRepository hrRepository;
    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    private final NotificationPublisher notificationPublisher;
    private final HrContextService hrContextService;

    /**
     * ✅ HR tạo meeting cho cả PROJECT (tất cả thực tập sinh trong project)
     */
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, Long hrUserId) {

        // Get HR
        Long hrId = hrContextService.getHrIdFromUserId(hrUserId);
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR"));

        // ✅ Get Program
        Project program = projectRepository.findById(request.getProgramId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình với ID: " + request.getProgramId()));

        // ✅ Lấy tất cả thực tập sinh trong program
        List<InternProfile> interns = internProfileRepository.findByProgram_Id(request.getProgramId());

        if (interns.isEmpty()) {
            throw new RuntimeException("Chương trình này chưa có thực tập sinh nào!");
        }

        // Create meeting in database
        Meeting meeting = Meeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .program(program)
                .createdBy(hr)
                .status("SCHEDULED")
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);

        // ✅ Đồng bộ lên Google Calendar cho TẤT CẢ thực tập sinh có tài khoản Google
        List<String> googleEventIds = new ArrayList<>();
        List<MeetingResponse.InternInfo> internInfos = new ArrayList<>();

        for (InternProfile intern : interns) {
            User internUser = intern.getUser();

            MeetingResponse.InternInfo internInfo = MeetingResponse.InternInfo.builder()
                    .internId(intern.getId())
                    .internName(intern.getFullName())
                    .internEmail(internUser != null ? internUser.getEmail() : null)
                    .authProvider(internUser != null ? internUser.getAuthProvider() : null)
                    .calendarSynced(false)
                    .build();

            // Chỉ sync cho user có Google auth
            if (internUser != null && "GOOGLE".equals(internUser.getAuthProvider())) {
                String accessToken = internUser.getGoogleAccessToken();

                if (accessToken != null && !accessToken.isEmpty()) {
                    try {
                        System.out.println("📅 Syncing to calendar for: " + internUser.getEmail());

                        String eventId = googleCalendarService.createEvent(
                                accessToken,
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartTime(),
                                request.getEndTime(),
                                request.getLocation(),
                                internUser.getEmail()
                        );

                        if (eventId != null && !eventId.isEmpty()) {
                            googleEventIds.add(eventId);
                            internInfo.setCalendarSynced(true);
                            System.out.println("✅ Calendar synced for: " + internUser.getEmail());
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Failed to sync calendar for " + internUser.getEmail() + ": " + e.getMessage());
                        // Continue với các intern khác
                    }
                }
            }

            internInfos.add(internInfo);

            // ✅ Gửi notification cho TẤT CẢ thực tập sinh
            if (internUser != null) {
                try {
                    String dateStr = request.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    notificationPublisher.publish(
                            internUser.getId().toString(),
                            "NEW_MEETING",
                            "📅 Lịch họp chương trình từ " + hr.getFullname(),
                            String.format("Chương trình: %s\n%s\n🕐 %s\n📍 %s",
                                    program.getTitle(),
                                    request.getTitle(),
                                    dateStr,
                                    request.getLocation() != null ? request.getLocation() : "Chưa xác định"
                            )
                    );
                } catch (Exception e) {
                    System.err.println("❌ Failed to send notification to " + internUser.getEmail());
                }
            }
        }

        // ✅ Lưu tất cả Google Event IDs (phân cách bằng dấu phẩy)
        if (!googleEventIds.isEmpty()) {
            savedMeeting.setGoogleEventIds(String.join(",", googleEventIds));
            meetingRepository.save(savedMeeting);
            System.out.println("✅ Saved " + googleEventIds.size() + " Google Event IDs");
        }

        // Build response
        MeetingResponse response = MeetingResponse.builder()
                .id(savedMeeting.getId())
                .title(savedMeeting.getTitle())
                .description(savedMeeting.getDescription())
                .startTime(savedMeeting.getStartTime())
                .endTime(savedMeeting.getEndTime())
                .location(savedMeeting.getLocation())
                .status(savedMeeting.getStatus())
                .programId(program.getId())
                .programTitle(program.getTitle())
                .interns(internInfos)
                .googleEventIds(googleEventIds)
                .hrId(hr.getId())
                .hrName(hr.getFullname())
                .createdAt(savedMeeting.getCreatedAt())
                .updatedAt(savedMeeting.getUpdatedAt())
                .build();

        System.out.println("========== MEETING CREATION SUMMARY ==========");
        System.out.println("📋 Meeting: " + savedMeeting.getTitle());
        System.out.println("🎯 Program: " + program.getTitle());
        System.out.println("👥 Total interns: " + interns.size());
        System.out.println("📅 Calendar synced: " + googleEventIds.size() + " interns");
        System.out.println("✉️ Notifications sent: " + interns.size() + " interns");
        System.out.println("============================================");

        return response;
    }

    /**
     * ✅ Cập nhật meeting (và sync lên Google Calendar)
     */
    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long hrUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy meeting"));

        // Kiểm tra quyền
        Long hrId = hrContextService.getHrIdFromUserId(hrUserId);
        if (!meeting.getCreatedBy().getId().equals(hrId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật meeting này");
        }

        // Cập nhật các field nếu có trong request
        if (request.getTitle() != null) {
            meeting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            meeting.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            meeting.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            meeting.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null) {
            meeting.setLocation(request.getLocation());
        }

        Meeting updatedMeeting = meetingRepository.save(meeting);

        // ✅ Cập nhật tất cả events trên Google Calendar
        if (meeting.getGoogleEventIds() != null && !meeting.getGoogleEventIds().isEmpty()) {
            String[] eventIds = meeting.getGoogleEventIds().split(",");
            List<InternProfile> interns = internProfileRepository.findByProgram_Id(meeting.getProgram().getId());

            int updatedCount = 0;
            for (InternProfile intern : interns) {
                User internUser = intern.getUser();
                if (internUser != null && "GOOGLE".equals(internUser.getAuthProvider())) {
                    String accessToken = internUser.getGoogleAccessToken();

                    if (accessToken != null && eventIds.length > updatedCount) {
                        try {
                            googleCalendarService.updateEvent(
                                    accessToken,
                                    eventIds[updatedCount],
                                    updatedMeeting.getTitle(),
                                    updatedMeeting.getDescription(),
                                    updatedMeeting.getStartTime(),
                                    updatedMeeting.getEndTime(),
                                    updatedMeeting.getLocation()
                            );
                            updatedCount++;
                        } catch (Exception e) {
                            System.err.println("❌ Không thể cập nhật calendar event cho " + internUser.getEmail());
                        }
                    }
                }
            }
            System.out.println("✅ Đã cập nhật " + updatedCount + " calendar events");
        }

        // ✅ Gửi thông báo cho tất cả thực tập sinh về thay đổi
        List<InternProfile> interns = internProfileRepository.findByProgram_Id(meeting.getProgram().getId());
        for (InternProfile intern : interns) {
            User internUser = intern.getUser();
            if (internUser != null) {
                try {
                    String dateStr = updatedMeeting.getStartTime()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    notificationPublisher.publish(
                            internUser.getId().toString(),
                            "MEETING_UPDATED",
                            "📅 Lịch họp đã được cập nhật",
                            String.format("Chương trình: %s\n%s\n🕐 %s\n📍 %s",
                                    meeting.getProgram().getTitle(),
                                    updatedMeeting.getTitle(),
                                    dateStr,
                                    updatedMeeting.getLocation() != null ? updatedMeeting.getLocation() : "Chưa xác định"
                            )
                    );
                } catch (Exception e) {
                    System.err.println("❌ Không thể gửi thông báo cập nhật cho " + internUser.getEmail());
                }
            }
        }

        return mapToResponse(updatedMeeting);
    }

    /**
     * ✅ Lấy danh sách meetings theo role
     */
    public List<MeetingListResponse> getAllMeetings(Long userId, String role) {
        List<Meeting> meetings;

        if ("HR".equals(role) || "ADMIN".equals(role)) {
            // HR/Admin xem tất cả meetings họ tạo
            Long hrId = hrContextService.getHrIdFromUserId(userId);
            meetings = meetingRepository.findByCreatedBy_Id(hrId);
        } else {
            // Intern xem meetings của các project mà họ tham gia
            InternProfile intern = internProfileRepository.findByUser_Id(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thực tập sinh"));

            if (intern.getProgram() != null) {
                meetings = meetingRepository.findByProgram_Id(intern.getProgram().getId());
            } else {
                meetings = new ArrayList<>();
            }
        }

        return meetings.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Lấy chi tiết một meeting
     */
    public MeetingResponse getMeetingById(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy meeting"));

        return mapToResponse(meeting);
    }

    /**
     * ✅ Xóa meeting (và xóa trên Google Calendar)
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

        // ✅ Xóa tất cả events trên Google Calendar
        if (meeting.getGoogleEventIds() != null && !meeting.getGoogleEventIds().isEmpty()) {
            String[] eventIds = meeting.getGoogleEventIds().split(",");
            List<InternProfile> interns = internProfileRepository.findByProgram_Id(meeting.getProgram().getId());

            int deletedCount = 0;
            for (InternProfile intern : interns) {
                User internUser = intern.getUser();
                if (internUser != null && "GOOGLE".equals(internUser.getAuthProvider())) {
                    String accessToken = internUser.getGoogleAccessToken();

                    if (accessToken != null && eventIds.length > deletedCount) {
                        try {
                            googleCalendarService.deleteEvent(accessToken, eventIds[deletedCount]);
                            deletedCount++;
                        } catch (Exception e) {
                            System.err.println("❌ Failed to delete calendar event for " + internUser.getEmail());
                        }
                    }
                }
            }
            System.out.println("✅ Deleted " + deletedCount + " calendar events");
        }

        meetingRepository.delete(meeting);
    }

    // ========== MAPPER METHODS ==========

    private MeetingResponse mapToResponse(Meeting meeting) {
        List<InternProfile> interns = internProfileRepository.findByProgram_Id(meeting.getProgram().getId());

        List<MeetingResponse.InternInfo> internInfos = interns.stream()
                .map(intern -> {
                    User user = intern.getUser();
                    return MeetingResponse.InternInfo.builder()
                            .internId(intern.getId())
                            .internName(intern.getFullName())
                            .internEmail(user != null ? user.getEmail() : null)
                            .authProvider(user != null ? user.getAuthProvider() : null)
                            .calendarSynced(user != null && "GOOGLE".equals(user.getAuthProvider()))
                            .build();
                })
                .collect(Collectors.toList());

        List<String> eventIds = meeting.getGoogleEventIds() != null
                ? Arrays.asList(meeting.getGoogleEventIds().split(","))
                : new ArrayList<>();

        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .programId(meeting.getProgram().getId())
                .programTitle(meeting.getProgram().getTitle())
                .interns(internInfos)
                .googleEventIds(eventIds)
                .hrId(meeting.getCreatedBy().getId())
                .hrName(meeting.getCreatedBy().getFullname())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    private MeetingListResponse mapToListResponse(Meeting meeting) {
        int internCount = internProfileRepository.findByProgram_Id(meeting.getProgram().getId()).size();

        return MeetingListResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .programTitle(meeting.getProgram().getTitle())
                .internCount(internCount)
                .createdBy(meeting.getCreatedBy().getFullname())
                .build();
    }
}