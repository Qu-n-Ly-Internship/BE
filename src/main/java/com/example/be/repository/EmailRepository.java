package com.example.be.repository;

import com.example.be.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailRepository extends JpaRepository<Email, Long> {
    Optional<Email> findByCode(String code);
}
