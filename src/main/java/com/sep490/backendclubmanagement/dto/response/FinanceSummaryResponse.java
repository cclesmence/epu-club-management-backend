package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for finance summary dashboard
 * Contains aggregated financial information for a club
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceSummaryResponse {

    private BigDecimal balance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalBudget;
    private BigDecimal remaining;
    private String currency;
    private Long walletId;

}

