package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateNewsRequest {
    private String title;
    private String content;
    private String thumbnailUrl;
    private String newsType;
    private Boolean isSpotlight;

    // Nếu là staff thì clubId có thể null
    private Long clubId;

    // true = lưu bản nháp, false = gửi yêu cầu (submit)
    private Boolean isDraft;
    private Long teamId;
}
