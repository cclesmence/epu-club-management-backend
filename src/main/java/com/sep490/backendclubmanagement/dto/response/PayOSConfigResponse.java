package com.sep490.backendclubmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayOSConfigResponse {
    private Long clubId;
    private String clientId; // Masked value for security (e.g., "1234****5678")
    private boolean active;
    private boolean configured;
}



