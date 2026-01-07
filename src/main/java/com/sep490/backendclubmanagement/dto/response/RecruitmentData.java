package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentData {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime endDate;
    private RecruitmentStatus status;
    private String requirements;
    private Long clubId;
    private List<RecruitmentQuestionData> questions;
    private List<TeamOptionData> teamOptions; // Danh sách team options cho phép sinh viên lựa chọn
    private Integer totalApplications; // Tổng số đơn ứng tuyển đã nộp
    private Integer acceptedApplications; // Số đơn đã được chấp nhận
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


