package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentorDepartmentRequest {
    private Long mentorId;
    private Long departmentId;
    private String mentorName;
    private String departmentName;
}
