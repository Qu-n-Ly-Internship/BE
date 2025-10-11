package com.example.be.config;

import com.example.be.entity.Permission;
import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.repository.PermissionRepository;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Starting DataInitializer...");

        // 1. Tạo Permissions trước
        createPermissionsIfNotExist();

        // 2. Tạo Roles với permissions
        createRolesIfNotExist();

        // 3. Fix existing users có role = NULL
        fixExistingUsersWithNullRole();

        // 4. Tạo admin account nếu chưa có
        createAdminIfNotExist();

        System.out.println("✅ DataInitializer completed!");
    }

    private void createPermissionsIfNotExist() {
        String[][] permissions = {
                // Dashboard
                {"VIEW_DASHBOARD", "Xem Dashboard", "DASHBOARD"},

                // Internship Management
                {"VIEW_INTERNSHIPS", "Xem danh sách thực tập", "INTERNSHIP_MANAGEMENT"},
                {"CREATE_INTERNSHIP", "Tạo thực tập mới", "INTERNSHIP_MANAGEMENT"},
                {"EDIT_INTERNSHIP", "Chỉnh sửa thực tập", "INTERNSHIP_MANAGEMENT"},
                {"DELETE_INTERNSHIP", "Xóa thực tập", "INTERNSHIP_MANAGEMENT"},

                // Student Management
                {"VIEW_STUDENTS", "Xem danh sách sinh viên", "STUDENT_MANAGEMENT"},
                {"CREATE_STUDENT", "Thêm sinh viên mới", "STUDENT_MANAGEMENT"},
                {"EDIT_STUDENT", "Chỉnh sửa sinh viên", "STUDENT_MANAGEMENT"},
                {"DELETE_STUDENT", "Xóa sinh viên", "STUDENT_MANAGEMENT"},



                // User Management
                {"MANAGE_USERS", "Quản lý người dùng", "USER_MANAGEMENT"},

                // Permission Management
                {"MANAGE_PERMISSIONS", "Quản lý phân quyền", "PERMISSION_MANAGEMENT"},

                // Reporting
                {"VIEW_REPORTS", "Xem báo cáo", "REPORTING"}
        };

        for (String[] perm : permissions) {
            if (permissionRepository.findByName(perm[0]).isEmpty()) {
                Permission permission = Permission.builder()
                        .name(perm[0])
                        .description(perm[1])
                        .module(perm[2])
                        .build();
                permissionRepository.save(permission);
                System.out.println("✅ Created permission: " + perm[0]);
            }
        }
    }

    private void createRolesIfNotExist() {
        // ADMIN - Có tất cả quyền
        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Quản trị viên hệ thống")
                    .permissions(Set.copyOf(permissionRepository.findAll()))
                    .build();
            roleRepository.save(adminRole);
            System.out.println("✅ Created role: ADMIN với tất cả permissions");
        }

        // HR - Quản lý user và thực tập sinh - role_id: 2
        if (roleRepository.findByName("HR").isEmpty()) {
            Set<Permission> hrPermissions = Set.of(
                    permissionRepository.findByName("VIEW_DASHBOARD").orElseThrow(),
                    permissionRepository.findByName("VIEW_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("CREATE_INTERNSHIP").orElseThrow(),
                    permissionRepository.findByName("EDIT_INTERNSHIP").orElseThrow(),
                    permissionRepository.findByName("VIEW_STUDENTS").orElseThrow(),
                    permissionRepository.findByName("CREATE_STUDENT").orElseThrow(),
                    permissionRepository.findByName("EDIT_STUDENT").orElseThrow(),
                    permissionRepository.findByName("MANAGE_USERS").orElseThrow(),
                    permissionRepository.findByName("VIEW_REPORTS").orElseThrow()
            );
            Role hrRole = Role.builder()
                    .name("HR")
                    .description("Nhân viên nhân sự")
                    .permissions(hrPermissions)
                    .build();
            roleRepository.save(hrRole);
            System.out.println("✅ Created role: HR");
        }

        // MENTOR - Theo dõi thực tập sinh - role_id:3
        if (roleRepository.findByName("MENTOR").isEmpty()) {
            Set<Permission> mentorPermissions = Set.of(
                    permissionRepository.findByName("VIEW_DASHBOARD").orElseThrow(),
                    permissionRepository.findByName("VIEW_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("EDIT_INTERNSHIP").orElseThrow(),
                    permissionRepository.findByName("VIEW_STUDENTS").orElseThrow(),
                    permissionRepository.findByName("EDIT_STUDENT").orElseThrow(),
                    permissionRepository.findByName("VIEW_REPORTS").orElseThrow()
            );
            Role mentorRole = Role.builder()
                    .name("MENTOR")
                    .description("Người hướng dẫn")
                    .permissions(mentorPermissions)
                    .build();
            roleRepository.save(mentorRole);
            System.out.println("✅ Created role: MENTOR");
        }

        // INTERN - Chỉ xem thông tin của mình - role_id:4
        if (roleRepository.findByName("INTERN").isEmpty()) {
            Set<Permission> internPermissions = Set.of(
                    permissionRepository.findByName("VIEW_DASHBOARD").orElseThrow(),
                    permissionRepository.findByName("VIEW_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("VIEW_STUDENTS").orElseThrow()
            );
            Role internRole = Role.builder()
                    .name("INTERN")
                    .description("Thực tập sinh")
                    .permissions(internPermissions)
                    .build();
            roleRepository.save(internRole);
            System.out.println("✅ Created role: INTERN");
        }

        // USER - Tài khoản mới đăng ký, chờ duyệt - role_id:5
        if (roleRepository.findByName("USER").isEmpty()) {
            // Người dùng mới không có quyền quản trị trước khi được duyệt
            Set<Permission> userPermissions = Set.of();
            Role userRole = Role.builder()
                    .name("USER")
                    .description("Người dùng mới đăng ký, chờ duyệt")
                    .permissions(userPermissions)
                    .build();
            roleRepository.save(userRole);
            System.out.println("✅ Created role: USER (role_id: " + userRole.getId() + ")");
        } else {
            Role existingUser = roleRepository.findByName("USER").get();
            System.out.println("✅ Role USER already exists (role_id: " + existingUser.getId() + ")");
        }
    }

    // 🔧 FIX: Sửa users có role = NULL
    private void fixExistingUsersWithNullRole() {
        var usersWithNullRole = userRepository.findAll().stream()
                .filter(user -> user.getRole() == null)
                .toList();

        if (usersWithNullRole.isEmpty()) {
            System.out.println("✅ No users with NULL role found");
            return;
        }

        System.out.println("🔧 Fixing " + usersWithNullRole.size() + " users with NULL role...");

        for (User user : usersWithNullRole) {
            String roleName = determineRoleFromEmail(user.getEmail());
            Role role = roleRepository.findByName(roleName).orElseThrow();

            user.setRole(role);

            // Set status nếu NULL
            if (user.getStatus() == null) {
                user.setStatus("ACTIVE");
            }
            if (user.getAuthProvider() == null) {
                user.setAuthProvider("LOCAL");
            }

            userRepository.save(user);
            System.out.println("✅ Fixed user: " + user.getEmail() + " -> Role: " + roleName);
        }
    }

    private String determineRoleFromEmail(String email) {
        String emailLower = email.toLowerCase();
        // Chỉ match chính xác "admin@" ở đầu hoặc có dấu phân cách rõ ràng
        if (emailLower.equals("admin@company.com") || emailLower.startsWith("admin@")) {
            return "ADMIN";
        } else if (emailLower.startsWith("hr@") || emailLower.contains("hr.")) {
            return "HR";
        } else if (emailLower.startsWith("mentor@") || emailLower.contains("mentor.")) {
            return "MENTOR";
        } else {
            return "USER";  // Default to USER - không tự động gán ADMIN
        }
    }

    private void createAdminIfNotExist() {
        if (userRepository.findByEmail("admin@company.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("USER").orElseThrow();

            User admin = User.builder()
                    .fullName("Admin")
                    .email("admin@company.com")
                    .password(passwordEncoder.encode("admin123"))
                    .authProvider("LOCAL")
                    .role(adminRole)
                    .status("ACTIVE")
                    .build();
            admin.setUsername("admin");

            userRepository.save(admin);
            System.out.println("✅ Created admin account: admin@company.com / admin123");
        }
    }
}