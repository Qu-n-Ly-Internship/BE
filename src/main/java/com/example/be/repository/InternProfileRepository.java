package com.example.be.repository;

import com.example.be.entity.InternProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InternProfileRepository extends JpaRepository<InternProfile, Long> {
    // TODO: Cần thêm quan hệ User vào InternProfile entity hoặc dùng query khác
    @Query(value = "SELECT ip.* FROM intern_profiles ip WHERE ip.user_id = :userId", nativeQuery = true)
    Optional<InternProfile> findByUser_Id(@Param("userId") Long userId);
}
