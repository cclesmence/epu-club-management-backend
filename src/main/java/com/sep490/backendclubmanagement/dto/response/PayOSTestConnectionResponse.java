package com.sep490.backendclubmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayOSTestConnectionResponse {
    private boolean connected;
    private String message;
}



