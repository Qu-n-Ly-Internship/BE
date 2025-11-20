package com.example.be.controller;

import com.example.be.config.JwtUtil;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import com.example.be.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;
    private final ProfileService profileService;

    // Lấy profile của user hiện tại
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Map<String, Object> profile = profileService.getMyProfile(authHeader);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Cập nhật profile
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Map<String, Object> result = profileService.updateProfile(authHeader, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // Helper method để extract email từ JWT token (deprecated - dùng ProfileService thay thế)
    @Deprecated
    private String extractEmailFromToken(String authHeader) {
        return profileService.extractEmailFromToken(authHeader);
    }
}