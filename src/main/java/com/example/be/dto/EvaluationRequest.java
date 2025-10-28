package com.example.be.dto;

import com.example.be.enums.CycleType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EvaluationRequest {
    private Long internId;
    private Long evaluatorId; // mentor hoặc HR
    private CycleType cycle;
    private Integer periodNo;
    private String comment;
    private List<ScoreRequest> scores;

    @Data
    public static class ScoreRequest {
        private String criteriaName;
        private BigDecimal score;
        private String comment;
    }
}

