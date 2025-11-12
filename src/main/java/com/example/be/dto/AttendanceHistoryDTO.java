package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceHistoryDTO {
    private List<AttendanceRecordDTO> data;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
