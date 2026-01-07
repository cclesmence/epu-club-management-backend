package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApproveNewsRequest {
    private String title;
    private String content;
    private String thumbnailUrl;
    private String newsType;
    private Boolean isSpotlight; // optional
    private String note;         // <<-- THÊM: ghi chú khi duyệt / publish

}
