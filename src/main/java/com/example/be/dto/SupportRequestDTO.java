package com.example.be.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequestDTO {
    private Integer internId;
    private Integer userId; // ⭐ THÊM
    private String subject;
    private String message;
    private String attachmentFileId;
    private String priority;
}