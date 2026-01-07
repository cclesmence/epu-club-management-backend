package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.ReportRequirementResponse;
import com.sep490.backendclubmanagement.entity.SubmissionReportRequirement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionReportRequirementMapper {

    /**
     * Map SubmissionReportRequirement entity to ReportRequirementResponse
     * Note: clubRequirements must be set separately as it comes from ClubReportRequirement
     */
    default ReportRequirementResponse toDto(SubmissionReportRequirement requirement) {
        if (requirement == null) {
            return null;
        }

        // Build createdBy info
        ReportRequirementResponse.UserInfo createdByInfo = null;
        if (requirement.getCreatedBy() != null) {
            createdByInfo = ReportRequirementResponse.UserInfo.builder()
                    .id(requirement.getCreatedBy().getId())
                    .fullName(requirement.getCreatedBy().getFullName())
                    .email(requirement.getCreatedBy().getEmail())
                    .studentCode(requirement.getCreatedBy().getStudentCode())
                    .build();
        }

        return ReportRequirementResponse.builder()
                .id(requirement.getId())
                .title(requirement.getTitle())
                .description(requirement.getDescription())
                .dueDate(requirement.getDueDate())
                .reportType(requirement.getReportType())
                .templateUrl(requirement.getTemplateUrl())
                .createdAt(requirement.getCreatedAt())
                .updatedAt(requirement.getUpdatedAt())
                .createdBy(createdByInfo)
                .build();
    }
}

