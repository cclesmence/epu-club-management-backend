package com.sep490.backendclubmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSemesterRequest {
    private String semesterName;
    private String semesterCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}

