package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class ClubFilterRequest extends PageableRequest {
    private String keyword; // Search by club name or club code
    private Long campusId;
    private Long categoryId;
    private String status;
}
