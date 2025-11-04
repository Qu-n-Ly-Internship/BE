package com.example.be.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDTO {
    private String email;           // Email của intern (dùng để tìm intern)
    private LocalDate startDate;    // Ngày bắt đầu nghỉ
    private LocalDate endDate;      // Ngày kết thúc nghỉ
    private String reason;          // Lý do nghỉ phép
}