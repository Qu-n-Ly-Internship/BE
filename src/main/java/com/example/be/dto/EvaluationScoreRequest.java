package com.example.be.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class EvaluationScoreRequest {
    private String criteriaName;
    private BigDecimal score;
    private String comment;
}
