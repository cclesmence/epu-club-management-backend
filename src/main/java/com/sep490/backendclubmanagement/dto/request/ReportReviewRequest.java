package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportReviewRequest {
    @NotNull(message = "Report ID is required")
    private Long reportId;

    @NotNull(message = "Status is required")
    private ReportStatus status; // APPROVED or REJECTED

    private String reviewerFeedback;

    private Boolean mustResubmit; // Optional: set to true if report must be resubmitted
}

