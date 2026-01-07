package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayOSConfigRequest {
    @NotBlank
    private String clientId;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String checksumKey;

    private Boolean active;
}



