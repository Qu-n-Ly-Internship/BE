package com.example.be.repository;

import com.example.be.entity.SupportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findByInternId(Integer internId);

    List<SupportRequest> findByUserId(Integer userId);

    List<SupportRequest> findByHrId(Long hrId);
    List<SupportRequest> findByStatus(String status);
    Page<SupportRequest> findAll(Pageable pageable);
}