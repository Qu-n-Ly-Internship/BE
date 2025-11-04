package com.example.be.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestStatsResponse {
    private long total;      // Tổng số đơn
    private long pending;    // Đơn chờ duyệt
    private long approved;   // Đơn đã duyệt
    private long rejected;   // Đơn bị từ chối

    // Tỷ lệ phần trăm
    public double getApprovalRate() {
        if (total == 0) return 0.0;
        return (double) approved / total * 100;
    }

    public double getRejectionRate() {
        if (total == 0) return 0.0;
        return (double) rejected / total * 100;
    }
}