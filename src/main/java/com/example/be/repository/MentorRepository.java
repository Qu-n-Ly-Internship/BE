package com.example.be.repository;

import com.example.be.entity.Department;
import com.example.be.entity.Mentors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorRepository extends JpaRepository<Mentors, Long> {
    Optional<Mentors> findByUser_Id(Long userId);

    // Tìm mentor theo department
    List<Mentors> findByDepartment(Department department);

    // Tìm mentor chưa thuộc department nào
    List<Mentors> findByDepartmentIsNull();
}
