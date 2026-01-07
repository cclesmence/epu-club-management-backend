package com.sep490.backendclubmanagement.dto.response.notification;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long id;

    private Long recipientId;
    private Long actorId;

    private String title;
    private String message;

    private String notificationType;
    private String priority;

    private Boolean isRead;
    private String actionUrl;

    private Long relatedClubId;
    private Long relatedNewsId;
    private Long relatedTeamId;
    private Long relatedRequestId;

    private Object metadata;

    private String createdAt;
}
