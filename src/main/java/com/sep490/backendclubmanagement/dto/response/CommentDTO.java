// src/main/java/com/sep490/backendclubmanagement/dto/response/CommentDTO.java
package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentDTO {
    private Long id;
    private Long postId;
    private Long parentId;
    private Long rootParentId;

    private Long userId;
    private String userName;
    private String userAvatar;

    private String content;
    private Boolean edited;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Chỉ dùng khi cần trả theo cây (thread)
    private List<CommentDTO> replies;
}
