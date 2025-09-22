package com.example.be.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private String fullName;
    private String role;  // mặc định INTERN nếu không chọn
}