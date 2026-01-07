package com.sep490.backendclubmanagement.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

















