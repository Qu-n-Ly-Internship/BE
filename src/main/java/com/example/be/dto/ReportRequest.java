package com.example.be.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
    private Long hrId;
    private Long internId;
    private BigDecimal overallScore;
    private String summary;
    private String recommendations;

}
