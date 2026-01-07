package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String title;
    private String message;

    private String notificationType;   // ví dụ: NEWS_PENDING_APPROVAL
    private String priority;           // LOW / NORMAL / HIGH / URGENT

    private Boolean read;              // isRead
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    private String actionUrl;          // link FE để redirect khi click

    // thông tin actor (người gây ra thông báo) – tuỳ frontend có dùng hay không
    private Long actorId;
    private String actorName;
    private String actorAvatarUrl;
}
