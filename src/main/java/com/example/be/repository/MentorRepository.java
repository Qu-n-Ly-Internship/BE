package com.example.be.repository;

import com.example.be.entity.Mentors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentorRepository extends JpaRepository<Mentors, Long> {
    Optional<Mentors> findByUser_Id(Long userId);

}
