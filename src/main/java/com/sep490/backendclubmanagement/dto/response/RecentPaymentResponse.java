package com.sep490.backendclubmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentPaymentResponse {
    private Long id;
    private String transactionCode;
    private String orderCode;
    private BigDecimal amount;
    private String description;
    private String paymentMethod;
    private String paymentStatus;
    private Boolean success;
    private LocalDateTime paymentTime;
    private LocalDateTime transactionDateTime;
    private String counterAccountName;
    private String counterAccountBankName;
    private String reference;
}



























