package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventApprovalRequest {
    
    @NotNull(message = "Request Event ID is required")
    private Long requestEventId;
    
    @NotNull(message = "Status is required")
    private RequestStatus status;
    
    private String responseMessage;
}

