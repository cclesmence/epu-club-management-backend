package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventWithoutReportRequirementDto {
    private Long eventId;
    private String eventTitle;
    private Long clubId;
    private String clubName;
}

