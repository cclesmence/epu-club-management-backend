package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PublicNewsDTO {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String excerpt;
    private LocalDateTime publishedAt;
    private Boolean spotlight;
}
