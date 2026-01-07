package com.sep490.backendclubmanagement.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    private String type;
    private String action;
    private T payload;
    private LocalDateTime timestamp;
    private String messageId;

    public static <T> WebSocketMessage<T> of(String type, String action, T payload) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .action(action)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .messageId(java.util.UUID.randomUUID().toString())
                .build();
    }
}

















