package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PageResp<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first;
    private boolean last;
}
