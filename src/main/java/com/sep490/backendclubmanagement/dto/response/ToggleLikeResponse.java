package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

/** Kết quả sau khi toggle: đã like hay chưa + tổng số like. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ToggleLikeResponse {
    private boolean liked; // true = sau thao tác, bài đang được tôi like
    private long count;    // tổng số like hiện tại của post
}

