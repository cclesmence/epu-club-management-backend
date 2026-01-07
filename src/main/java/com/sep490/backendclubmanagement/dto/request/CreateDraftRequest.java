package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateDraftRequest {
    private String title;
    private String content;
    private String thumbnailUrl;
    private String newsType;
    // BỎ isSpotlight khỏi nháp
    // THÊM clubId: bắt buộc cho Chủ nhiệm/Trưởng ban, optional cho Staff
    private Long clubId;
    private Long teamId;

}
