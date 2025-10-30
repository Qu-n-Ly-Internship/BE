package com.example.be.dto;

import com.example.be.entity.Report;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long reportId;
    private String hrName;
    private String internName;
    private BigDecimal overallScore;
    private String summary;
    private String recommendations;
    private LocalDateTime createdAt;

    // ReportResponse.java
    public static ReportResponse fromEntity(Report r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .hrName(r.getHr().getFullname())
                .internName(r.getIntern().getFullName())
                .overallScore(r.getOverallScore())
                .summary(r.getSummary())
                .recommendations(r.getRecommendations())
                .createdAt(r.getCreatedAt())
                .build();
    }

}
