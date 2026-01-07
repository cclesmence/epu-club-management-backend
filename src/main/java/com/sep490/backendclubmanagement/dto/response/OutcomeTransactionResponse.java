package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutcomeTransactionResponse {
    private Long id;
    private String transactionCode;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private String recipient;
    private String purpose;
    private TransactionStatus status;
    private String notes;
    private String receiptUrl;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName;
}

