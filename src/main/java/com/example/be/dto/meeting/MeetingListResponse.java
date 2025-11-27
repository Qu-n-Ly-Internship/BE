package com.example.be.dto.meeting;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingListResponse {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String status;
    private String programTitle;
    private Integer internCount;
    private String createdBy;
}