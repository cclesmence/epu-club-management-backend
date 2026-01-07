package com.sep490.backendclubmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_is_read", columnList = "is_read"),
    @Index(name = "idx_notification_created_at", columnList = "created_at"),
    @Index(name = "idx_notification_type", columnList = "notification_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor; // User who triggered the notification (optional)

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationPriority priority;
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private java.time.LocalDateTime readAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    // Related entity references (optional, for easier querying)
    @Column(name = "related_club_id")
    private Long relatedClubId;

    @Column(name = "related_post_id")
    private Long relatedPostId;

    @Column(name = "related_event_id")
    private Long relatedEventId;

    @Column(name = "related_news_id")
    private Long relatedNewsId;

    @Column(name = "related_fee_id")
    private Long relatedFeeId;

    @Column(name = "related_recruitment_id")
    private Long relatedRecruitmentId;

    @Column(name = "related_report_id")
    private Long relatedReportId;

    @Column(name = "related_team_id")
    private Long relatedTeamId;

    // Additional metadata stored as JSON
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    @Builder.Default
    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false; // Whether notification was sent via WebSocket

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Helper methods
    public void markAsRead() {
        this.isRead = true;
        this.readAt = java.time.LocalDateTime.now();
    }

    public void markAsSent() {
        this.isSent = true;
        this.sentAt = java.time.LocalDateTime.now();
    }
}

