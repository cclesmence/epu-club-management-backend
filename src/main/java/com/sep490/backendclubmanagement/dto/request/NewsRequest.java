package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class NewsRequest extends  PageableRequest {
    private String keyword;
    private String newsType;
    private Long clubId;
}
