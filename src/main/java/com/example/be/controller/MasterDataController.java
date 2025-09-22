package com.example.be.controller;

import com.example.be.entity.University;
import com.example.be.entity.Department;
import com.example.be.entity.Major;
import com.example.be.entity.HR;
import com.example.be.repository.UniversityRepository;
import com.example.be.repository.DepartmentRepository;
import com.example.be.repository.MajorRepository;
import com.example.be.repository.HRRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
public class MasterDataController {

    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final HRRepository hrRepository;

    // ============ UNIVERSITIES ============
    @GetMapping("/universities")
    public ResponseEntity<?> getAllUniversities() {
        try {
            List<University> universities = universityRepository.findAll();
            return ResponseEntity.ok(universities);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách trường: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/universities")
    public ResponseEntity<?> createUniversity(@RequestBody University university) {
        try {
            University savedUniversity = universityRepository.save(university);
            return ResponseEntity.ok(savedUniversity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo trường: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/universities/{id}")
    public ResponseEntity<?> updateUniversity(@PathVariable Long id, @RequestBody University university) {
        try {
            if (!universityRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            university.setUniId(id);
            University updatedUniversity = universityRepository.save(university);
            return ResponseEntity.ok(updatedUniversity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật trường: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/universities/{id}")
    public ResponseEntity<?> deleteUniversity(@PathVariable Long id) {
        try {
            if (!universityRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            universityRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa trường thành công"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa trường: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============ DEPARTMENTS ============
    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentRepository.findAll();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách phòng ban: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        try {
            Department savedDepartment = departmentRepository.save(department);
            return ResponseEntity.ok(savedDepartment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo phòng ban: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            if (!departmentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            department.setDepartmentId(id);
            Department updatedDepartment = departmentRepository.save(department);
            return ResponseEntity.ok(updatedDepartment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật phòng ban: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            if (!departmentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            departmentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa phòng ban thành công"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa phòng ban: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============ MAJORS ============
    @GetMapping("/majors")
    public ResponseEntity<?> getAllMajors() {
        try {
            List<Major> majors = majorRepository.findAll();
            return ResponseEntity.ok(majors);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách ngành học: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/majors")
    public ResponseEntity<?> createMajor(@RequestBody Major major) {
        try {
            Major savedMajor = majorRepository.save(major);
            return ResponseEntity.ok(savedMajor);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo ngành học: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/majors/{id}")
    public ResponseEntity<?> updateMajor(@PathVariable Long id, @RequestBody Major major) {
        try {
            if (!majorRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            major.setMajorId(id);
            Major updatedMajor = majorRepository.save(major);
            return ResponseEntity.ok(updatedMajor);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật ngành học: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/majors/{id}")
    public ResponseEntity<?> deleteMajor(@PathVariable Long id) {
        try {
            if (!majorRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            majorRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa ngành học thành công"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa ngành học: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============ HR ============
    @GetMapping("/hr")
    public ResponseEntity<?> getAllHR() {
        try {
            List<HR> hrList = hrRepository.findAll();
            return ResponseEntity.ok(hrList);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách HR: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/hr")
    public ResponseEntity<?> createHR(@RequestBody HR hr) {
        try {
            HR savedHR = hrRepository.save(hr);
            return ResponseEntity.ok(savedHR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo HR: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/hr/{id}")
    public ResponseEntity<?> updateHR(@PathVariable Long id, @RequestBody HR hr) {
        try {
            if (!hrRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            hr.setHrId(id);
            HR updatedHR = hrRepository.save(hr);
            return ResponseEntity.ok(updatedHR);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật HR: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/hr/{id}")
    public ResponseEntity<?> deleteHR(@PathVariable Long id) {
        try {
            if (!hrRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            hrRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa HR thành công"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa HR: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}