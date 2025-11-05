package com.example.be.repository;

import com.example.be.entity.UsedQrSignature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedQrSignatureRepository extends JpaRepository<UsedQrSignature, Long> {
    boolean existsBySignature(String signature);
}
