package com.example.be.repository;

import com.example.be.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByMentor_Id(Long mentorId);

    // ✅ Lọc tất cả project theo department (qua mentor)
    List<Project> findByMentorDepartmentId(Long departmentId);

    // ✅ Lọc tất cả project theo program (qua department → mentor)
    List<Project> findByMentorDepartmentProgramId(Long programId);
}
