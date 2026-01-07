package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Simplified response for officer report requirements
 * Contains only essential information for better performance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerReportRequirementResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private ReportType reportType;
    private String templateUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName;  // Just the name, not full user object

    private SimplifiedClubRequirement clubRequirement;  // Single requirement for the specific club

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimplifiedClubRequirement {
        private Long id;
        private Long clubId;
        private Long teamId;
        private SimplifiedReport report;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimplifiedReport {
        private Long id;
        private String status;
        private Boolean mustResubmit;
        private Long createdBy;
        private String createdByUserName;
    }
}
