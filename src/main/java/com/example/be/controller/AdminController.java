package com.example.be.controller;

import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Tạo user mới với email và password
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");
            String role = request.get("role");

            // Kiểm tra email đã tồn tại
            if (userRepository.findByEmail(email).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email đã tồn tại!");
                return ResponseEntity.badRequest().body(error);
            }

            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUsername(email); // Sử dụng email làm username
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setStatus("ACTIVE"); // Tạo ngay với trạng thái ACTIVE

            User savedUser = userRepository.save(user);

            // Trả về user đã tạo (không bao gồm password)
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("fullName", savedUser.getFullName());
            response.put("email", savedUser.getEmail());
            response.put("role", savedUser.getRole());
            response.put("status", savedUser.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Tạo tài khoản thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy danh sách users với filter và phân trang
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "role", defaultValue = "") String role,
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);

            // Simple filtering (you can enhance this with custom queries)
            var filteredUsers = users.getContent().stream()
                    .filter(user -> query.isEmpty() ||
                            user.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(query.toLowerCase()))
                    .filter(user -> role.isEmpty() || user.getRole().equals(role))
                    .filter(user -> status.isEmpty() || user.getStatus().equals(status))
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("fullName", user.getFullName());
                        userMap.put("email", user.getEmail());
                        userMap.put("role", user.getRole());
                        userMap.put("status", user.getStatus());
                        return userMap;
                    }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("content", filteredUsers);
            response.put("total", (long) filteredUsers.size());
            response.put("totalPages", 1);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Cập nhật thông tin user
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            var userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy user!");
                return ResponseEntity.badRequest().body(error);
            }

            User user = userOpt.get();

            if (request.containsKey("fullName")) {
                user.setFullName(request.get("fullName"));
            }
            if (request.containsKey("role")) {
                user.setRole(request.get("role"));
            }
            if (request.containsKey("status")) {
                user.setStatus(request.get("status"));
            }

            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Cập nhật thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Cập nhật thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Xóa user
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy user!");
                return ResponseEntity.badRequest().body(error);
            }

            userRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Xóa thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Duyệt user (từ PENDING -> ACTIVE) - giữ lại method cũ
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        try {
            var userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy user!");
                return ResponseEntity.badRequest().body(error);
            }

            User user = userOpt.get();
            user.setStatus("ACTIVE");
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User " + user.getEmail() + " đã được duyệt!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Duyệt thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Khóa user (ACTIVE -> INACTIVE) - cập nhật method cũ
    @PutMapping("/disable/{id}")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            var userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy user!");
                return ResponseEntity.badRequest().body(error);
            }

            User user = userOpt.get();
            user.setStatus("INACTIVE");
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User " + user.getEmail() + " đã bị khóa!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Khóa thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
