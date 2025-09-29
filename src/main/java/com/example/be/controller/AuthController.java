package com.example.be.controller;

import com.example.be.dto.LoginRequest;
import com.example.be.dto.RegisterRequest;
import com.example.be.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")

    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginWithResponse(request));
    }
}