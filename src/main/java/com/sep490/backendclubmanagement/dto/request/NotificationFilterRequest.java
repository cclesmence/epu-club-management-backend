package com.sep490.backendclubmanagement.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationFilterRequest {
    private Boolean unreadOnly;
    private String type;
    private String priority;
}
