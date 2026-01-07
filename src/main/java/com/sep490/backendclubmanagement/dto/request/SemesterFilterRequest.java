package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class SemesterFilterRequest extends PageableRequest {
    private Boolean isCurrent;
    private String keyword;
}
