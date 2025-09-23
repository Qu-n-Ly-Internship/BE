package com.example.be.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String role;
    private String status;
}