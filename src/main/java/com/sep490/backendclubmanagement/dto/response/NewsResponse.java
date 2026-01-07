package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponse {
    @Builder.Default   private long total = 0;
    @Builder.Default private long count = 0;
    @Builder.Default
    private List<NewsData> data = new ArrayList<>();
}
