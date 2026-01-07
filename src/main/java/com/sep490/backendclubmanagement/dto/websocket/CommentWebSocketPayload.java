package com.sep490.backendclubmanagement.dto.websocket;

import com.sep490.backendclubmanagement.dto.response.CommentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentWebSocketPayload {
    private CommentDTO comment;
    private Long postId;
    private String action; // NEW, EDIT, DELETE
}

