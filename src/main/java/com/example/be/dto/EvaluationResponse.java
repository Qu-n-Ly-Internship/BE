package com.example.be.dto;

import com.example.be.entity.Mentors;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {
    private Long evaluationId;
    private Long internId;
    private String internName;
    private String comment;
    private String cycle;
    private String mentorName;
    private Integer periodNo;
    private LocalDateTime createdAt;
    private List<EvaluationScoreResponse> scores;
}
