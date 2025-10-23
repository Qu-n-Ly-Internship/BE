package com.example.be.repository;

import com.example.be.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByMentor_Id(Long mentorId);
}
