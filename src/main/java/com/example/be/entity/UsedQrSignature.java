package com.example.be.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "used_qr_signatures",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"signature"})
        }
)
public class UsedQrSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "signature", length = 128, nullable = false)
    private String signature;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "used_by_ip", length = 45)
    private String usedByIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
