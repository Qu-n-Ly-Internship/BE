package com.example.be.repository;

import com.example.be.entity.Hr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HrRepository extends JpaRepository<Hr, Long> {
    @Query(value = "SELECT h.* FROM hrs h WHERE h.user_id = :userId", nativeQuery = true)
    Optional<Hr> findByUser_Id(@Param("userId") Long userId);
}