package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    List<Notification> findTop10ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findTop10ByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(Long userId);

}
    