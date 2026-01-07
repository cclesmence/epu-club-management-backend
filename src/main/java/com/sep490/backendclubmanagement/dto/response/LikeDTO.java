package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Thông tin 1 lượt like (dùng cho list). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LikeDTO {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}