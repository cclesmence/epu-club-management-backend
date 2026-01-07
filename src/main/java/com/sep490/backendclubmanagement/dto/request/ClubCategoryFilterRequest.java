package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class ClubCategoryFilterRequest extends PageableRequest {
    private String keyword;
}

