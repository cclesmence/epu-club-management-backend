package com.sep490.backendclubmanagement.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostMediaData {
    private Long id;
    private String title;
    private String mediaUrl;
    private String mediaType;
    private String caption;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
