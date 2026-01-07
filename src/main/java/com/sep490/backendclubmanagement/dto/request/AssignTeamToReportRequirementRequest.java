package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTeamToReportRequirementRequest {
    
    @NotNull(message = "Club Report Requirement ID is required")
    private Long clubReportRequirementId;
    
    @NotNull(message = "Team ID is required")
    private Long teamId;
}

