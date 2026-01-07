package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest extends PageableRequest {
    private String keyword;
    private Long eventTypeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long clubId;
}
