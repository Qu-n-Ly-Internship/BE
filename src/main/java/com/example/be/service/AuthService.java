package com.example.be.service;

import com.example.be.dto.LoginRequest;
import com.example.be.dto.RegisterRequest;
import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email đã tồn tại!";
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "Username đã tồn tại!";
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole() != null ? request.getRole().toUpperCase() : "INTERN");
        user.setStatus("PENDING");

        userRepository.save(user);
        return "Đăng ký thành công! Role: " + user.getRole();
    }

    public String login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    if (!"ACTIVE".equals(user.getStatus())) {
                        return "Tài khoản chưa được duyệt!";
                    }
                    return "Đăng nhập thành công! Xin chào " + user.getUsername()
                            + " (Role: " + user.getRole() + ")";
                })
                .orElse("Sai email hoặc mật khẩu!");
    }

    // Phương thức mới trả về object cho frontend
    public Map<String, Object> loginWithResponse(LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            var userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Email không tồn tại!");
                return response;
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                response.put("success", false);
                response.put("message", "Mật khẩu không đúng!");
                return response;
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                response.put("success", false);
                response.put("message", "Tài khoản chưa được kích hoạt hoặc đã bị khóa!");
                return response;
            }

            // Mock JWT token (bạn có thể implement JWT thật)
            String token = "mock-jwt-token-" + user.getId() + "-" + System.currentTimeMillis();

            response.put("success", true);
            response.put("message", "Đăng nhập thành công!");
            response.put("token", token);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "status", user.getStatus()
            ));

            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return response;
        }
    }
}