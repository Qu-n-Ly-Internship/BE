package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceReportDTO {
    private Long internId;
    private String internName;
    private String employeeId;
    private String department;
    private Long totalWorkingDays;
    private Long totalLeaveDays;
    private Long totalLateDays;
    private Long totalAbsentDays;
}