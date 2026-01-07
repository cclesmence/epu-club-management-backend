package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequirementFilterRequest extends PageableRequest {
    private ReportType reportType;
    private Long clubId; // Filter by club ID (if requirement is for specific club)
    private String keyword; // Search by title or description
}

