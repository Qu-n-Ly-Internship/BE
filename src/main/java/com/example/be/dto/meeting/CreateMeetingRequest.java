package com.example.be.dto.meeting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {

    @NotNull(message = "Intern ID không được để trống")
    private Long internId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được quá 200 ký tự")
    private String title;

    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Size(max = 200, message = "Địa điểm không được quá 200 ký tự")
    private String location;
}