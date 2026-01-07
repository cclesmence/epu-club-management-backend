package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequirementResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private ReportType reportType;
    private String templateUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserInfo createdBy;

    private List<ClubRequirementInfo> clubRequirements;
    private Integer clubCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClubRequirementInfo {
        private Long id;
        private Long clubId;
        private String clubName;
        private String clubCode;
        private String status;
        private Long teamId;
        private ReportInfo report;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String studentCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportInfo {
        private Long id;
        private String reportTitle;
        private String status;
        private LocalDateTime submittedDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean mustResubmit;
        private UserInfo createdBy;
    }
}

