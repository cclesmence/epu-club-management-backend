package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Combined DTO for both Income and Outcome transactions
 * Used for displaying all transactions together in the UI
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String code; // reference (income) or transactionCode (outcome)
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionType type;
    private TransactionStatus status;

    // Income specific fields
    private String source;
    private Long feeId;
    private String feeTitle;

    // Outcome specific fields
    private String recipient;
    private String purpose;
    private String receiptUrl;

    // Common fields
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TransactionType {
        INCOME, OUTCOME
    }
}

