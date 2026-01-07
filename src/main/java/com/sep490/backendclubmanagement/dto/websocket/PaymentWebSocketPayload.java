package com.sep490.backendclubmanagement.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebSocketPayload {
    private Long orderCode;
    private String transactionCode;
    private BigDecimal amount;
    private String status;
    private String message;
    private Long userId;
    private Long feeId;
}

















