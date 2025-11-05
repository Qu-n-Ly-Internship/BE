package com.example.be.notification.repository;

import com.example.be.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // ← Spring's transactional

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndStatus(String userId, String status);

    long countByUserIdAndStatus(String userId, String status);

    @Modifying
    @Transactional // ← Dùng Spring's @Transactional
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.userId = ?1 AND n.status = 'UNREAD'")
    void markAllAsReadByUserId(String userId);
}