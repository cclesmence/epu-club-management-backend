package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterResponse {
    private Long id;
    private String semesterName;
    private String semesterCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}

