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
        // 1. Tạo Permissions
        createPermissionsIfNotExist();

        // 2. Tạo Roles với permissions
        createRolesIfNotExist();

        // 3. Tạo admin account
        createAdminIfNotExist();
    }

    private void createPermissionsIfNotExist() {
        String[][] permissions = {
                { "READ_USERS", "Xem danh sách users", "USER_MANAGEMENT" },
                { "CREATE_USERS", "Tạo user mới", "USER_MANAGEMENT" },
                { "UPDATE_USERS", "Cập nhật thông tin user", "USER_MANAGEMENT" },
                { "DELETE_USERS", "Xóa user", "USER_MANAGEMENT" },
                { "MANAGE_ROLES", "Quản lý vai trò", "USER_MANAGEMENT" },
                { "MANAGE_PERMISSIONS", "Quản lý quyền", "USER_MANAGEMENT" },

                { "READ_INTERNSHIPS", "Xem thông tin thực tập", "INTERNSHIP_MANAGEMENT" },
                { "CREATE_INTERNSHIPS", "Tạo chương trình thực tập", "INTERNSHIP_MANAGEMENT" },
                { "UPDATE_INTERNSHIPS", "Cập nhật thông tin thực tập", "INTERNSHIP_MANAGEMENT" },
                { "APPROVE_INTERNSHIPS", "Duyệt thực tập sinh", "INTERNSHIP_MANAGEMENT" },

                { "VIEW_REPORTS", "Xem báo cáo", "REPORTING" },
                { "EXPORT_DATA", "Xuất dữ liệu", "REPORTING" }
        };

        for (String[] perm : permissions) {
            if (permissionRepository.findByName(perm[0]).isEmpty()) {
                Permission permission = Permission.builder()
                        .name(perm[0])
                        .description(perm[1])
                        .module(perm[2])
                        .build();
                permissionRepository.save(permission);
                System.out.println("✅ Đã tạo permission: " + perm[0]);
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
            System.out.println("✅ Đã tạo role: ADMIN với tất cả permissions");
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
                    permissionRepository.findByName("VIEW_REPORTS").orElseThrow());
            Role hrRole = Role.builder()
                    .name("HR")
                    .description("Nhân viên nhân sự")
                    .permissions(hrPermissions)
                    .build();
            roleRepository.save(hrRole);
            System.out.println("✅ Đã tạo role: HR");
        }

        // MENTOR - Theo dõi thực tập sinh
        if (roleRepository.findByName("MENTOR").isEmpty()) {
            Set<Permission> mentorPermissions = Set.of(
                    permissionRepository.findByName("READ_USERS").orElseThrow(),
                    permissionRepository.findByName("READ_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("UPDATE_INTERNSHIPS").orElseThrow(),
                    permissionRepository.findByName("VIEW_REPORTS").orElseThrow());
            Role mentorRole = Role.builder()
                    .name("MENTOR")
                    .description("Người hướng dẫn")
                    .permissions(mentorPermissions)
                    .build();
            roleRepository.save(mentorRole);
            System.out.println("✅ Đã tạo role: MENTOR");
        }

        // INTERN - Chỉ xem thông tin của mình
        if (roleRepository.findByName("INTERN").isEmpty()) {
            Set<Permission> internPermissions = Set.of(
                    permissionRepository.findByName("READ_INTERNSHIPS").orElseThrow());
            Role internRole = Role.builder()
                    .name("INTERN")
                    .description("Thực tập sinh")
                    .permissions(internPermissions)
                    .build();
            roleRepository.save(internRole);
            System.out.println("✅ Đã tạo role: INTERN");
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
            System.out.println("✅ Đã tạo admin account: admin@company.com / admin123");
        }
    }
}