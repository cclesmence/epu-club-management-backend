package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentData {
    private Long id;
    private String content;
    private Boolean isEdited;
    private Long parentCommentId;
    private Long rootParentCommentId;

    private Long userId;
    private String userName;

    private LocalDateTime createdAt;
}
