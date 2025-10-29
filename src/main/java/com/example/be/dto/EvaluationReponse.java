package com.example.be.dto;


import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationReponse {
    private Long evaluationId;
    private String comment;
    private String cycle;
    private Integer periodNo;
    private LocalDateTime createdAt;
    private String mentorName;
    private String hrName;
    private List<ScoreResponse> scores;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreResponse {
        private String criteriaName;
        private BigDecimal score;
        private String comment;
    }
}


