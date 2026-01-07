package com.sep490.backendclubmanagement.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeWebSocketPayload {
    private Long postId;
    private Long userId;
    private String userFullName;
    private String userAvatarUrl;
    private boolean liked; // true = liked, false = unliked
    private long totalLikes;
}

