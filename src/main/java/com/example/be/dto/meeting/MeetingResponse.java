package com.example.be.dto.meeting;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String status;
    private List<String> googleEventIds;

    // Program info
    private Long programId;
    private String programTitle;

    // Interns info
    private List<InternInfo> interns;

    // HR info
    private Long hrId;
    private String hrName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternInfo {
        private Long internId;
        private String internName;
        private String internEmail;
        private String authProvider;
        private boolean calendarSynced;
    }
}