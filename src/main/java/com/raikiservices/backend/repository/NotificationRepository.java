package com.raikiservices.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Notifications visibles par un compte : les siennes, plus les notifications globales. */
    @Query("SELECT n FROM Notification n WHERE n.recipientId IS NULL OR n.recipientId = :userId")
    Page<Notification> findVisible(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND :userId NOT MEMBER OF n.readBy
            """)
    Page<Notification> findVisibleUnread(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND :userId NOT MEMBER OF n.readBy
            """)
    List<Notification> findAllVisibleUnread(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND :userId NOT MEMBER OF n.readBy
            """)
    long countUnread(@Param("userId") Long userId);
}
