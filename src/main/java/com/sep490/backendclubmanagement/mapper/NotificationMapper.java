package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.notification.NotificationDTO;
import com.sep490.backendclubmanagement.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDTO toDto(Notification n) {
        if (n == null) return null;

        return NotificationDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipient().getId())
                .actorId(n.getActor() != null ? n.getActor().getId() : null)

                .title(n.getTitle())
                .message(n.getMessage())

                .notificationType(n.getNotificationType().name())
                .priority(n.getPriority().name())

                .isRead(n.getIsRead())
                .actionUrl(n.getActionUrl())

                .relatedClubId(n.getRelatedClubId())
                .relatedNewsId(n.getRelatedNewsId())
                .relatedTeamId(n.getRelatedTeamId())
                .relatedRequestId(n.getRelatedReportId())

                .metadata(n.getMetadata())

                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .build();
    }
}
