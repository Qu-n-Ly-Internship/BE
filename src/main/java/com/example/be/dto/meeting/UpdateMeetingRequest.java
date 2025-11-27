package com.example.be.dto.meeting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {

    @Size(max = 200, message = "Tiêu đề không được quá 200 ký tự")
    private String title;

    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Size(max = 200, message = "Địa điểm không được quá 200 ký tự")
    private String location;
}