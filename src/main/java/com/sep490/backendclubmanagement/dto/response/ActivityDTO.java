package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDTO {
    private Long id;
    private String type; // "POST" or "NEWS"
    private String title;
    private String authorName;
    private LocalDateTime createdAt;
}