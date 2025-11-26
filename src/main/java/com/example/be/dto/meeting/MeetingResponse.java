package com.example.be.dto.meeting;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private String googleEventId;

    // Intern info
    private Long internId;
    private String internName;
    private String internEmail;

    // HR info
    private Long hrId;
    private String hrName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}