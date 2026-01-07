package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class CampusFilterRequest extends PageableRequest {
    private String keyword;
}

