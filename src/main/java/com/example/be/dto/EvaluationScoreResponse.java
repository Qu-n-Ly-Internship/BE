package com.example.be.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScoreResponse {
    private String criteriaName;
    private BigDecimal score;
    private String comment;
}
