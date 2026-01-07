package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitReportRequest {
    
    @NotNull(message = "Report ID is required")
    private Long reportId;
}

