package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportListItemResponse {
    private Long id;
    private String reportTitle;
    private String content;
    private String fileUrl;
    private ReportStatus status;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private Boolean mustResubmit;
    private LocalDateTime createdAt;

    private ClubMiniInfo club;
    private SemesterMiniInfo semester;
    private UserMiniInfo createdBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClubMiniInfo {
        private Long id;
        private String clubName;
        private String clubCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterMiniInfo {
        private Long id;
        private String semesterName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserMiniInfo {
        private Long id;
        private String fullName;
        private String email;
    }
}

