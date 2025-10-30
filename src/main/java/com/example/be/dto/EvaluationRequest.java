package com.example.be.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class EvaluationRequest {
    private Long evaluationId;

    private Long userId;
    private Long internId;
    private String comment;
    private String cycle; // chỉ dùng cho mentor (weekly / monthly)
    private Integer periodNo;
    private List<EvaluationScoreRequest> scores;
}

