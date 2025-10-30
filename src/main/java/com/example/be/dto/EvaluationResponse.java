package com.example.be.dto;

import com.example.be.entity.Evaluation;
import com.example.be.entity.Mentors;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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


    public static EvaluationResponse fromEntity(Evaluation e) {
        return EvaluationResponse.builder()
                .evaluationId(e.getEvaluationId())
                .internId(e.getIntern().getId())
                .internName(e.getIntern().getFullName())
                .mentorName(e.getMentorEvaluator().getFullName())
                .cycle(e.getCycle())
                .comment(e.getComment())
                .periodNo(e.getPeriodNo())
                .createdAt(e.getCreatedAt())
                .scores(e.getScores().stream()
                        .map(s -> new EvaluationScoreResponse(
                                s.getCriteriaName(),
                                s.getScore(),
                                s.getComment()))
                        .collect(Collectors.toList()))
                .build();
    }

}
