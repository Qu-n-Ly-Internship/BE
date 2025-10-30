// src/main/java/com/example/be/dto/TaskDTO.java
package com.example.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private String status;     // PENDING, IN_PROGRESS, DONE
    private String deadline;   // "2025-12-31"
}