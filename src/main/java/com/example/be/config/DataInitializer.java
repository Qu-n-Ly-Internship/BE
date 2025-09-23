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
                {"READ_USERS", "Xem danh sách users", "USER_MANAGEMENT"},
                {"CREATE_USERS", "Tạo user mới", "USER_MANAGEMENT"},
                {"UPDATE_USERS", "Cập nhật thông tin user", "USER_MANAGEMENT"},
                {"DELETE_USERS", "Xóa user", "USER_MANAGEMENT"},
                {"MANAGE_ROLES", "Quản lý vai trò", "USER_MANAGEMENT"},
                {"MANAGE_PERMISSIONS", "Quản lý quyền", "USER_MANAGEMENT"},

                {"READ_INTERNSHIPS", "Xem thông tin thực tập", "INTERNSHIP_MANAGEMENT"},
                {"CREATE_INTERNSHIPS", "Tạo chương trình thực tập", "INTERNSHIP_MANAGEMENT"},
                {"UPDATE_INTERNSHIPS", "Cập nhật thông tin thực tập", "INTERNSHIP_MANAGEMENT"},
                {"APPROVE_INTERNSHIPS", "Duyệt thực tập sinh", "INTERNSHIP_MANAGEMENT"},

                {"VIEW_REPORTS", "Xem báo cáo", "REPORTING"},
                {"EXPORT_DATA", "Xuất dữ liệu", "REPORTING"}
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

        // HR - Quản lý user và thực tập sinh
        if (roleRepository.findByName("HR").isEmpty()) {
            Set<Permission> hrPermissions = Set.of(
                    permissionRepository.findByName("READ_USERS").orElseThrow(),
                    permissionRepository.findByName("CREATE_USERS").orElseThrow(),
                    permissionRepository.findByName("UPDATE_USERS").orElseThrow(),
                    permissionRepository.findByName("READ_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("CREATE_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("APPROVE_INTERNSHIPS").orElseThrow(),
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

        // MENTOR - Theo dõi thực tập sinh
        if (roleRepository.findByName("MENTOR").isEmpty()) {
            Set<Permission> mentorPermissions = Set.of(
                    permissionRepository.findByName("READ_USERS").orElseThrow(),
                    permissionRepository.findByName("READ_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("UPDATE_INTERNSHIPS").orElseThrow(),
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

        // INTERN - Chỉ xem thông tin của mình
        if (roleRepository.findByName("INTERN").isEmpty()) {
            Set<Permission> internPermissions = Set.of(
                    permissionRepository.findByName("READ_INTERNSHIPS").orElseThrow()
            );
            Role internRole = Role.builder()
                    .name("INTERN")
                    .description("Thực tập sinh")
                    .permissions(internPermissions)
                    .build();
            roleRepository.save(internRole);
            System.out.println("✅ Created role: INTERN");
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

            userRepository.save(user);
            System.out.println("✅ Fixed user: " + user.getEmail() + " -> Role: " + roleName);
        }
    }

    private String determineRoleFromEmail(String email) {
        if (email.toLowerCase().contains("admin")) {
            return "ADMIN";
        } else if (email.toLowerCase().contains("hr")) {
            return "HR";
        } else if (email.toLowerCase().contains("mentor")) {
            return "MENTOR";
        } else {
            return "INTERN";  // Default
        }
    }

    private void createAdminIfNotExist() {
        if (userRepository.findByEmail("admin@company.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@company.com")
                    .username("admin@company.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .status("ACTIVE")
                    .build();

            userRepository.save(admin);
            System.out.println("✅ Created admin account: admin@company.com / admin123");
        }
    }
}