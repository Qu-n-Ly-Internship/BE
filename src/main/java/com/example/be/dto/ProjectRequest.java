package com.example.be.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {
    private Long id;
    private String title;
    private Integer capacity;
    private String description;

    private Long mentorId;
    private String mentorName;

    private List<String> internNames; // danh sách tên intern tham gia chương trình
}
