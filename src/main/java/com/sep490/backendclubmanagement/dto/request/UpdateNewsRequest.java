package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class UpdateNewsRequest {
    private String title;         // optional
    private String content;       // optional
    private String thumbnailUrl;  // optional
    private String newsType;      // optional
}