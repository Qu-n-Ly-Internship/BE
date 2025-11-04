package com.example.be.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveReviewRequest {
    private String hrEmail;          // Email của HR (để xác định người duyệt)
    private String rejectionReason;  // Lý do từ chối (chỉ dùng khi reject)
}
