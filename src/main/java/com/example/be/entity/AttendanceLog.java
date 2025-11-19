package com.example.be.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Builder

@Getter
@Setter
@Entity
@Table(name = "attendance_logs")
@NoArgsConstructor
@AllArgsConstructor
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
    private LocalDateTime eventTime ;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(length = 128)
    private String sig;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
    }

    public enum EventType {
        SCAN, CHECKIN, CHECKOUT, FAILED_SIG, EXPIRED
    }
}
