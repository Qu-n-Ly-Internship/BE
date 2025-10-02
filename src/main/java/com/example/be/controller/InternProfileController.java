package com.example.be.controller;

import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intern-profiles")
@RequiredArgsConstructor
public class InternProfileController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    // 1. Lấy danh sách intern profiles với filter
    @GetMapping("")
    public ResponseEntity<?> getAllProfiles(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "school", required = false) String school,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT ip.intern_id, ip.fullname as student, ip.email as studentEmail,
                       u.name_uni as school, ip.major_id, ip.year_of_study,
                       ip.phone, ip.available_from as startDate, ip.end_date as endDate,
                       p.title, ip.status
                FROM intern_profiles ip
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                LEFT JOIN intern_programs p ON ip.program_id = p.program_id
                WHERE 1=1
                """);

            List<Object> params = new java.util.ArrayList<>();

            // Tìm kiếm theo tên hoặc email
            if (!query.trim().isEmpty()) {
                sql.append(" AND (ip.fullname LIKE ? OR ip.email LIKE ?)");
                String searchPattern = "%" + query.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // Lọc theo trường
            if (school != null && !school.trim().isEmpty()) {
                sql.append(" AND u.name_uni = ?");
                params.add(school);
            }

            // Lọc theo ngành (major_id)
            if (major != null && !major.trim().isEmpty()) {
                sql.append(" AND ip.major_id = ?");
                params.add(Integer.parseInt(major));
            }

            // Lọc theo trạng thái
            if (status != null && !status.trim().isEmpty()) {
                sql.append(" AND ip.status = ?");
                params.add(status);
            }

            sql.append(" ORDER BY ip.intern_id DESC LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            List<Map<String, Object>> profiles = jdbcTemplate.queryForList(
                    sql.toString(),
                    params.toArray()
            );

            // Map major_id sang tên ngành
            profiles.forEach(p -> {
                Object majorId = p.get("major_id");
                p.put("major", getMajorName(majorId));
            });

            int total = getTotalProfileCount(query, school, major, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", profiles,
                    "pagination", Map.of(
                            "currentPage", page,
                            "pageSize", size,
                            "totalElements", total,
                            "totalPages", (int) Math.ceil((double) total / size)
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải danh sách: " + e.getMessage()
            ));
        }
    }

    // 2. Tạo intern profile mới
    @PostMapping("")
    public ResponseEntity<?> createProfile(@RequestBody Map<String, Object> request) {
        try {
            String student = (String) request.get("student");
            String studentEmail = (String) request.get("studentEmail");
            String school = (String) request.get("school");
            String major = (String) request.get("major");
            String title = (String) request.get("title");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String status = (String) request.getOrDefault("status", "active");

            // Validate
            if (student == null || student.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập tên sinh viên"
                ));
            }

            if (studentEmail == null || studentEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Vui lòng nhập email"
                ));
            }

            // Kiểm tra email đã tồn tại chưa
            String checkEmailSql = "SELECT COUNT(*) FROM intern_profiles WHERE email = ?";
            int count = jdbcTemplate.queryForObject(checkEmailSql, Integer.class, studentEmail);
            if (count > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email đã tồn tại trong hệ thống"
                ));
            }

            // Tìm hoặc tạo university
            Integer uniId = null;
            if (school != null && !school.trim().isEmpty()) {
                String findUniSql = "SELECT uni_id FROM universities WHERE name_uni = ?";
                List<Map<String, Object>> uniResult = jdbcTemplate.queryForList(findUniSql, school);

                if (!uniResult.isEmpty()) {
                    uniId = (Integer) uniResult.get(0).get("uni_id");
                } else {
                    // Tạo university mới
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    jdbcTemplate.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO universities (name_uni) VALUES (?)",
                                Statement.RETURN_GENERATED_KEYS
                        );
                        ps.setString(1, school);
                        return ps;
                    }, keyHolder);
                    uniId = keyHolder.getKey().intValue();
                }
            }

            // Map tên ngành sang major_id
            Integer majorId = getMajorId(major);

            // Tạo intern_program nếu có title
            Integer programId = null;
            if (title != null && !title.trim().isEmpty()) {
                KeyHolder programKeyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO intern_programs (title, description, capacity, start_date, end_date) VALUES (?, '', 1, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, title.trim());
                    ps.setString(2, startDate);
                    ps.setString(3, endDate);
                    return ps;
                }, programKeyHolder);
                programId = programKeyHolder.getKey().intValue();
            }

            // Insert intern profile
            String sql = """
                INSERT INTO intern_profiles 
                (fullname, email, uni_id, major_id, program_id, available_from, end_date, status, phone, year_of_study)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, '', 0)
                """;

            jdbcTemplate.update(sql,
                    student.trim(),
                    studentEmail.trim(),
                    uniId,
                    majorId,
                    programId,
                    startDate,
                    endDate,
                    status
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thêm thực tập sinh thành công!"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thêm thất bại: " + e.getMessage()
            ));
        }
    }

    // 3. Cập nhật intern profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request
    ) {
        try {
            // Kiểm tra profile tồn tại
            String checkSql = "SELECT COUNT(*) FROM intern_profiles WHERE intern_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, id);

            if (count == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy thực tập sinh với ID: " + id
                ));
            }

            String student = (String) request.get("student");
            String studentEmail = (String) request.get("studentEmail");
            String school = (String) request.get("school");
            String major = (String) request.get("major");
            String title = (String) request.get("title");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String status = (String) request.get("status");

            // Tìm hoặc tạo university
            Integer uniId = null;
            if (school != null && !school.trim().isEmpty()) {
                String findUniSql = "SELECT uni_id FROM universities WHERE name_uni = ?";
                List<Map<String, Object>> uniResult = jdbcTemplate.queryForList(findUniSql, school);

                if (!uniResult.isEmpty()) {
                    uniId = (Integer) uniResult.get(0).get("uni_id");
                } else {
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    jdbcTemplate.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO universities (name_uni) VALUES (?)",
                                Statement.RETURN_GENERATED_KEYS
                        );
                        ps.setString(1, school);
                        return ps;
                    }, keyHolder);
                    uniId = keyHolder.getKey().intValue();
                }
            }

            Integer majorId = getMajorId(major);

            // Lấy program_id hiện tại
            String getProgramSql = "SELECT program_id FROM intern_profiles WHERE intern_id = ?";
            Integer currentProgramId = jdbcTemplate.queryForObject(getProgramSql, Integer.class, id);

            // Cập nhật hoặc tạo program mới nếu title thay đổi
            Integer programId = currentProgramId;
            if (title != null && !title.trim().isEmpty()) {
                if (currentProgramId != null) {
                    // Update existing program
                    jdbcTemplate.update(
                            "UPDATE intern_programs SET title = ?, start_date = ?, end_date = ? WHERE program_id = ?",
                            title.trim(), startDate, endDate, currentProgramId
                    );
                } else {
                    // Create new program
                    KeyHolder programKeyHolder = new GeneratedKeyHolder();
                    jdbcTemplate.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO intern_programs (title, description, capacity, start_date, end_date) VALUES (?, '', 1, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS
                        );
                        ps.setString(1, title.trim());
                        ps.setString(2, startDate);
                        ps.setString(3, endDate);
                        return ps;
                    }, programKeyHolder);
                    programId = programKeyHolder.getKey().intValue();
                }
            }

            String sql = """
                UPDATE intern_profiles 
                SET fullname = ?, 
                    email = ?, 
                    uni_id = ?, 
                    major_id = ?,
                    program_id = ?,
                    available_from = ?, 
                    end_date = ?, 
                    status = ?
                WHERE intern_id = ?
                """;

            jdbcTemplate.update(sql,
                    student,
                    studentEmail,
                    uniId,
                    majorId,
                    programId,
                    startDate,
                    endDate,
                    status,
                    id
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Cập nhật thất bại: " + e.getMessage()
            ));
        }
    }

    // 4. Xóa intern profile
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            String sql = "DELETE FROM intern_profiles WHERE intern_id = ?";
            int affected = jdbcTemplate.update(sql, id);

            if (affected == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy thực tập sinh với ID: " + id
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Xóa thất bại: " + e.getMessage()
            ));
        }
    }

    // Helper methods
    private int getTotalProfileCount(String query, String school, String major, String status) {
        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(*) FROM intern_profiles ip
            LEFT JOIN universities u ON ip.uni_id = u.uni_id
            WHERE 1=1
            """);

        List<Object> params = new java.util.ArrayList<>();

        if (!query.trim().isEmpty()) {
            countSql.append(" AND (ip.fullname LIKE ? OR ip.email LIKE ?)");
            String searchPattern = "%" + query.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (school != null && !school.trim().isEmpty()) {
            countSql.append(" AND u.name_uni = ?");
            params.add(school);
        }

        if (major != null && !major.trim().isEmpty()) {
            countSql.append(" AND ip.major_id = ?");
            params.add(Integer.parseInt(major));
        }

        if (status != null && !status.trim().isEmpty()) {
            countSql.append(" AND ip.status = ?");
            params.add(status);
        }

        return jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());
    }

    private String getMajorName(Object majorId) {
        if (majorId == null) return "";
        int id = (Integer) majorId;
        return switch (id) {
            case 1 -> "Công nghệ thông tin";
            case 2 -> "Khoa học máy tính";
            case 3 -> "Kỹ thuật điện tử";
            case 4 -> "Kỹ thuật phần mềm";
            case 5 -> "An toàn thông tin";
            default -> "Chuyên ngành khác";
        };
    }

    private Integer getMajorId(String majorName) {
        if (majorName == null || majorName.trim().isEmpty()) return null;

        return switch (majorName.trim()) {
            case "Công nghệ thông tin" -> 1;
            case "Khoa học máy tính" -> 2;
            case "Kỹ thuật điện tử" -> 3;
            case "Kỹ thuật phần mềm" -> 4;
            case "An toàn thông tin" -> 5;
            default -> 1; // Default
        };
    }
}