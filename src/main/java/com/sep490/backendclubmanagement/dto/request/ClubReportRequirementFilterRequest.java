package com.sep490.backendclubmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubReportRequirementFilterRequest extends PageableRequest {
    private String status; // OVERDUE, UNSUBMITTED, DRAFT, PENDING_CLUB, etc.
    private Long semesterId; // Filter by semester (based on deadline)
    private String keyword; // Search by title or description
    private Long teamId; // Filter by team ID (for team officer to see only their assigned requirements)
}

