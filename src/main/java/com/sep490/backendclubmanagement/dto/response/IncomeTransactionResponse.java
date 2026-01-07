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
public class IncomeTransactionResponse {
    private Long id;
    private String reference;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private String source;
    private TransactionStatus status;
    private String notes;
    private String receiptUrl;

    // Related entities info
    private Long feeId;
    private String feeTitle;
    private Long userId;
    private String userName;
    private String userEmail;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName;
}

