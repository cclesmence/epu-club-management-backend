// In: dto/response/UpcomingEventDTO.java
package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpcomingEventDTO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private String location;
    private String clubName;
    private String imageUrl; // Ảnh bìa sự kiện
}