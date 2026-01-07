package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReportRequirementRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDateTime dueDate;

    private String templateUrl;
}

