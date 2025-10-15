package com.example.be.controller;

import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.repository.RoleRepository;
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
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Tạo user mới với email và password
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");
            String roleName = request.get("role") != null ? request.get("role").toUpperCase() : "INTERN";

            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại!"));
            }

            // ✅ FIX: Đảm bảo fullName không bị NULL
            if (fullName == null || fullName.trim().isEmpty()) {
                String emailPrefix = email.split("@")[0];
                fullName = emailPrefix.substring(0, 1).toUpperCase() + emailPrefix.substring(1);
            }

            // ✅ Generate username from email (required field)
            String username = email.split("@")[0];
            
            // ✅ Check if username already exists, add suffix if needed
            String finalUsername = username;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = username + suffix;
                suffix++;
            }

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUsername(finalUsername); // ✅ Set username with uniqueness check
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setStatus("ACTIVE");
            user.setAuthProvider("LOCAL"); // ✅ Set authProvider

            User savedUser = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("fullName", savedUser.getFullName());
            response.put("email", savedUser.getEmail());
            response.put("role", savedUser.getRole().getName());
            response.put("status", savedUser.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tạo tài khoản thất bại: " + e.getMessage()));
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

            var filteredUsers = users.getContent().stream()
                    .filter(user -> query.isEmpty() ||
                            user.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(query.toLowerCase()))
                    .filter(user -> role.isEmpty() || user.getRole().getName().equalsIgnoreCase(role))
                    .filter(user -> status.isEmpty() || user.getStatus().equalsIgnoreCase(status))
                    .map(user -> Map.of(
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "email", user.getEmail(),
                            "role", user.getRole().getName(),
                            "status", user.getStatus()
                    )).toList();

        long totalUsers = userRepository.count();

        return ResponseEntity.ok(Map.of(
            "content", filteredUsers,
            "total", filteredUsers.size(),
            "totalPages", 1,
            // total number of users in the system (for dashboard/summary)
            "totalUsers", totalUsers
        ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể tải danh sách: " + e.getMessage()));
        }
    }

    // Cập nhật thông tin user
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

            // ✅ FIX: Ensure username is set for existing users that might not have it
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String username = user.getEmail().split("@")[0];
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.findByUsername(finalUsername).isPresent()) {
                    finalUsername = username + suffix;
                    suffix++;
                }
                user.setUsername(finalUsername);
            }

            if (request.containsKey("fullName")) {
                user.setFullName(request.get("fullName"));
            }
            if (request.containsKey("role")) {
                String roleName = request.get("role").toUpperCase();
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));
                user.setRole(role);
            }
            if (request.containsKey("status")) {
                user.setStatus(request.get("status"));
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }

    // Xóa user
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy user!"));
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Xóa thất bại: " + e.getMessage()));
        }
    }

    // Duyệt user
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
            
            // ✅ FIX: Ensure username is set for existing users
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String username = user.getEmail().split("@")[0];
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.findByUsername(finalUsername).isPresent()) {
                    finalUsername = username + suffix;
                    suffix++;
                }
                user.setUsername(finalUsername);
            }
            
            user.setStatus("ACTIVE");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User " + user.getEmail() + " đã được duyệt!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Duyệt thất bại: " + e.getMessage()));
        }
    }

    // Khóa user
    @PutMapping("/disable/{id}")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
            
            // ✅ FIX: Ensure username is set for existing users
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String username = user.getEmail().split("@")[0];
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.findByUsername(finalUsername).isPresent()) {
                    finalUsername = username + suffix;
                    suffix++;
                }
                user.setUsername(finalUsername);
            }
            
            user.setStatus("INACTIVE");
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User " + user.getEmail() + " đã bị khóa!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Khóa thất bại: " + e.getMessage()));
        }
    }

    // Reset user về role USER và status PENDING (dùng khi user bị gán sai role)
    @PutMapping("/reset/{id}")
    public ResponseEntity<?> resetUserToDefault(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
            
            // ✅ FIX: Ensure username is set for existing users
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String username = user.getEmail().split("@")[0];
                String finalUsername = username;
                int suffix = 1;
                while (userRepository.findByUsername(finalUsername).isPresent()) {
                    finalUsername = username + suffix;
                    suffix++;
                }
                user.setUsername(finalUsername);
            }
            
            // Reset về role USER
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER không tồn tại!"));
            user.setRole(userRole);
            user.setStatus("PENDING");
            
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "Đã reset user " + user.getEmail() + " về role USER và status PENDING!",
                "user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "role", user.getRole().getName(),
                    "status", user.getStatus()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reset thất bại: " + e.getMessage()));
        }
    }
}
