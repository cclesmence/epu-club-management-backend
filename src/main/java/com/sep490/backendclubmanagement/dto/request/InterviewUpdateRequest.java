package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class InterviewUpdateRequest {
    @NotNull
    public Long applicationId;

    public LocalDateTime interviewTime;
    public String interviewAddress;
    public String interviewPreparationRequirements;
}

