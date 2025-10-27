package com.example.be.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternRequest {
    private Long id;
    private String fullName;
}
