package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentorDepartmentDTO {
    private Long mentorId;
    private String mentorName;
    private String mentorEmail;
    private Long departmentId;
    private String departmentName;
}
