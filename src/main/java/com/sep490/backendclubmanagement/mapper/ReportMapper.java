package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.ReportDetailResponse;
import com.sep490.backendclubmanagement.dto.response.ReportListItemResponse;
import com.sep490.backendclubmanagement.entity.Report;
import com.sep490.backendclubmanagement.entity.SubmissionReportRequirement;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ReportMapper {

    /**
     * Map Report entity to ReportListItemResponse
     */
    default ReportListItemResponse toListItem(Report report) {
        if (report == null) {
            return null;
        }

        ReportListItemResponse.ClubMiniInfo clubInfo = null;
        if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getClub() != null) {
            clubInfo = ReportListItemResponse.ClubMiniInfo.builder()
                    .id(report.getClubReportRequirement().getClub().getId())
                    .clubName(report.getClubReportRequirement().getClub().getClubName())
                    .clubCode(report.getClubReportRequirement().getClub().getClubCode())
                    .build();
        }

        ReportListItemResponse.SemesterMiniInfo semesterInfo = null;
        if (report.getSemester() != null) {
            semesterInfo = ReportListItemResponse.SemesterMiniInfo.builder()
                    .id(report.getSemester().getId())
                    .semesterName(report.getSemester().getSemesterName())
                    .build();
        }

        ReportListItemResponse.UserMiniInfo userInfo = null;
        if (report.getCreatedBy() != null) {
            userInfo = ReportListItemResponse.UserMiniInfo.builder()
                    .id(report.getCreatedBy().getId())
                    .fullName(report.getCreatedBy().getFullName())
                    .email(report.getCreatedBy().getEmail())
                    .build();
        }

        return ReportListItemResponse.builder()
                .id(report.getId())
                .reportTitle(report.getReportTitle())
                .content(report.getContent())
                .fileUrl(report.getFileUrl())
                .status(report.getStatus())
                .submittedDate(report.getSubmittedDate())
                .reviewedDate(report.getReviewedDate())
                .mustResubmit(report.isMustResubmit())
                .createdAt(report.getCreatedAt())
                .club(clubInfo)
                .semester(semesterInfo)
                .createdBy(userInfo)
                .build();
    }

    /**
     * Map Report entity to ReportDetailResponse
     */
    default ReportDetailResponse toDetail(Report report) {
        if (report == null) {
            return null;
        }

        ReportDetailResponse.ClubInfo clubInfo = null;
        if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getClub() != null) {
            clubInfo = ReportDetailResponse.ClubInfo.builder()
                    .id(report.getClubReportRequirement().getClub().getId())
                    .clubName(report.getClubReportRequirement().getClub().getClubName())
                    .clubCode(report.getClubReportRequirement().getClub().getClubCode())
                    .build();
        }

        ReportDetailResponse.SemesterInfo semesterInfo = null;
        if (report.getSemester() != null) {
            semesterInfo = ReportDetailResponse.SemesterInfo.builder()
                    .id(report.getSemester().getId())
                    .semesterName(report.getSemester().getSemesterName())
                    .semesterCode(report.getSemester().getSemesterCode())
                    .build();
        }

        ReportDetailResponse.UserInfo userInfo = null;
        if (report.getCreatedBy() != null) {
            userInfo = ReportDetailResponse.UserInfo.builder()
                    .id(report.getCreatedBy().getId())
                    .fullName(report.getCreatedBy().getFullName())
                    .email(report.getCreatedBy().getEmail())
                    .studentCode(report.getCreatedBy().getStudentCode())
                    .build();
        }

        ReportDetailResponse.ReportRequirementInfo requirementInfo = null;
        if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getSubmissionReportRequirement() != null) {
            SubmissionReportRequirement submissionRequirement = report.getClubReportRequirement().getSubmissionReportRequirement();
            ReportDetailResponse.UserInfo requirementCreatedByInfo = null;
            if (submissionRequirement.getCreatedBy() != null) {
                requirementCreatedByInfo = ReportDetailResponse.UserInfo.builder()
                        .id(submissionRequirement.getCreatedBy().getId())
                        .fullName(submissionRequirement.getCreatedBy().getFullName())
                        .email(submissionRequirement.getCreatedBy().getEmail())
                        .studentCode(submissionRequirement.getCreatedBy().getStudentCode())
                        .build();
            }

            requirementInfo = ReportDetailResponse.ReportRequirementInfo.builder()
                    .id(submissionRequirement.getId())
                    .title(submissionRequirement.getTitle())
                    .description(submissionRequirement.getDescription())
                    .dueDate(submissionRequirement.getDueDate())
                    .reportType(submissionRequirement.getReportType())
                    .templateUrl(submissionRequirement.getTemplateUrl())
                    .createdBy(requirementCreatedByInfo)
                    .build();
        }

        return ReportDetailResponse.builder()
                .id(report.getId())
                .reportTitle(report.getReportTitle())
                .content(report.getContent())
                .fileUrl(report.getFileUrl())
                .status(report.getStatus())
                .submittedDate(report.getSubmittedDate())
                .reviewedDate(report.getReviewedDate())
                .reviewerFeedback(report.getReviewerFeedback())
                .mustResubmit(report.isMustResubmit())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .club(clubInfo)
                .semester(semesterInfo)
                .createdBy(userInfo)
                .reportRequirement(requirementInfo)
                .build();
    }
}

