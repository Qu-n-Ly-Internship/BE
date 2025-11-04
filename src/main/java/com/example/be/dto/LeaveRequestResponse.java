package com.example.be.dto;

import com.example.be.entity.LeaveRequest;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestResponse {
    private Long id;
    private Long internId;
    private String internName;
    private String internEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private Long leaveDays;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewerName;
    private String rejectionReason;

    // Chuyển từ Entity sang DTO
    public static LeaveRequestResponse fromEntity(LeaveRequest entity) {
        long leaveDays = ChronoUnit.DAYS.between(
                entity.getStartDate(),
                entity.getEndDate()
        ) + 1;

        return LeaveRequestResponse.builder()
                .id(entity.getId())
                .internId(entity.getIntern().getId())
                .internName(entity.getIntern().getFullName())
                .internEmail(entity.getIntern().getEmail())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .leaveDays(leaveDays)
                .createdAt(entity.getCreatedAt())
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
}