package com.example.be.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequestResponse {
    
    private Long id;
    
    private Integer internId;

    private Integer userID;
    
    private Long hrId;
    
    private String subject;
    
    private String message;
    
    private String attachmentFileId;
    
    private String status;
    
    private String priority;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime respondedAt;
    
    private LocalDateTime resolvedAt;
    
    private String hrResponse;
}
