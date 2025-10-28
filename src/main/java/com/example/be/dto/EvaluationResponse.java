package com.example.be.dto;

import com.example.be.enums.CycleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class EvaluationResponse {
    private Long evaluationId;
    private Long internId;
    private String internName;
    private String evaluatorName;
    private CycleType cycle;
    private Integer periodNo;
    private String comment;
    private LocalDateTime createdAt;
    private List<ScoreResponse> scores;

    @Data
    @Builder
    public static class ScoreResponse {
        private String criteriaName;
        private BigDecimal score;
        private String comment;
    }
}
