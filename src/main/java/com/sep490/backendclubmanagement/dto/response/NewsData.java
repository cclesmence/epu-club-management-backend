package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsData {
    private Long id;
    private String title;
    private String content;
    private String thumbnailUrl;
    private String newsType;

    // Đổi từ isDraft -> draft để MapStruct nhận diện property đúng chuẩn JavaBeans
    private Boolean draft;

    private Long clubId;
    private String clubName;

    // Chuỗi thời gian đã format
    private String updatedAt;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorRole; // "STAFF" hoặc "CLUB"

    private Boolean hidden;   // true = tin đang bị ẩn
    private Boolean deleted;
}
