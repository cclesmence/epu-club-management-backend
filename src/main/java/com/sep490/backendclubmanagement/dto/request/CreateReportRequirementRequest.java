package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequirementRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    private ReportType reportType;

    private String templateUrl;

    @NotEmpty(message = "At least one club ID is required")
    private List<Long> clubIds;

    private Long eventId; // Optional: if this requirement is related to an event
}

