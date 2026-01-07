package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentApplicationData {
    private Long id;
    private Long recruitmentId;
    private Long applicantId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String studentId;
    private Long teamId;
    private String teamName;
    private String clubName;
    private String recruitmentTitle;
    private RecruitmentApplicationStatus status;
    private String reviewNotes;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private LocalDateTime interviewTime;
    private String interviewAddress;
    private String interviewPreparationRequirements;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApplicationAnswerData> answers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApplicationAnswerData {
        private Long questionId;
        private String questionText;
        private String answerText;
        private String fileUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
