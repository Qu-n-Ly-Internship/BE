package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternResponse {
    private Long id;
    private String fullName;
    private String email;
    private Long projectId; // project hiện tại, null nếu không có
    private String status;
}
