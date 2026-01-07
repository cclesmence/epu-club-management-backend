package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWebSocketPayload {
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private String actionUrl;
    private Object metadata;
}
