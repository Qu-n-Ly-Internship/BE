package com.example.be.repository;

import com.example.be.entity.InternProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternProfileRepository extends JpaRepository<InternProfile, Long> {
}