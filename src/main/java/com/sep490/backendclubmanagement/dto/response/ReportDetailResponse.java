package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.ReportStatus;
import com.sep490.backendclubmanagement.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailResponse {
    private Long id;
    private String reportTitle;
    private String content;
    private String fileUrl;
    private ReportStatus status;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private String reviewerFeedback;
    private Boolean mustResubmit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ClubInfo club;
    private SemesterInfo semester;
    private UserInfo createdBy;
    private ReportRequirementInfo reportRequirement;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClubInfo {
        private Long id;
        private String clubName;
        private String clubCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterInfo {
        private Long id;
        private String semesterName;
        private String semesterCode;
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
    public static class ReportRequirementInfo {
        private Long id;
        private String title;
        private String description;
        private LocalDateTime dueDate;
        private ReportType reportType;
        private String templateUrl;
        private UserInfo createdBy;
    }
}

