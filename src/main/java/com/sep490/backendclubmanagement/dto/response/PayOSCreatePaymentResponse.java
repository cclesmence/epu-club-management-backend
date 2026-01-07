package com.sep490.backendclubmanagement.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class PayOSCreatePaymentResponse {
    private Long orderCode;
    private String paymentLink;
    private String qrCode;
    private Map<String, Object> raw;
}
