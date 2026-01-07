package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for listing applications (without answers)
 * Used to reduce data transfer when displaying application lists
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentApplicationListData {
    private Long id;
    private Long recruitmentId;
    private Long applicantId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String studentId;
    private String avatar;
    private Long teamId;
    private String teamName;
    private RecruitmentApplicationStatus status;
    private String reviewNotes;
    private LocalDateTime submittedDate;
    private LocalDateTime interviewTime;
    private String interviewAddress;
    private String interviewPreparationRequirements;
}
