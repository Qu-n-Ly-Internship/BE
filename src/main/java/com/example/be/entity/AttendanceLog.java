package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Builder

@Getter
@Setter
@Entity
@Table(name = "attendance_logs")
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false)
    private EventType eventType; // SCAN, CHECKIN, CHECKOUT, FAILED_SIG, EXPIRED

    @Column(name = "event_time")
    private LocalDateTime eventTime = LocalDateTime.now();

    @Lob
    private String payload;

    @Column(length = 128)
    private String sig;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Lob
    private String notes;

    public enum EventType {
        SCAN, CHECKIN, CHECKOUT, FAILED_SIG, EXPIRED
    }
}
