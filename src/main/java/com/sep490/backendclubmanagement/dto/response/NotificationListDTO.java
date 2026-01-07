package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListDTO {
    private long total;
    private List<com.sep490.backendclubmanagement.dto.response.notification.NotificationDTO> items;
}
