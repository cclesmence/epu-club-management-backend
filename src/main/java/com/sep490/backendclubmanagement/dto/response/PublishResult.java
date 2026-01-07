package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublishResult {
    private Long newsId;
    private NewsData news;
    private String message;
}
