package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class StaffFilterRequest extends PageableRequest {
    private Boolean isActive;
    private String keyword;
}


