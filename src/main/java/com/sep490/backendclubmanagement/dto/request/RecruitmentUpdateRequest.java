package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class RecruitmentUpdateRequest {
    @NotBlank
    public String title;
    public String description;
    @NotNull @Future
    public LocalDateTime endDate;
    public String requirements;
    public RecruitmentStatus status; // DRAFT or OPEN
    public List<RecruitmentQuestionRequest> questions;
    @NotEmpty(message = "Phải chọn ít nhất một phòng ban cho đợt tuyển dụng")
    public List<Long> teamOptionIds; // Danh sách ID của các team cho phép sinh viên lựa chọn
}


