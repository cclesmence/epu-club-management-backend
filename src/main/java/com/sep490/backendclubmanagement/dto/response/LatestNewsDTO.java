// In: dto/response/LatestNewsDTO.java
package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LatestNewsDTO {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String excerpt; // Đoạn trích ngắn
    private LocalDateTime createdAt;
}