package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.FinanceSummaryResponse;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for club finance operations
 * Accessible by club members
 */
@RestController
@RequestMapping("/api/clubs/{clubId}/finance")
@RequiredArgsConstructor
@Slf4j
public class ClubFinanceController {

    private final ClubWalletService clubWalletService;

    /**
     * Get finance summary for a club
     * GET /api/clubs/{clubId}/finance/summary
     *
     * Returns:
     * - balance: Current wallet balance (remaining money)
     * - totalIncome: Total income from all SUCCESS income transactions
     * - totalExpense: Total expense from all SUCCESS outcome transactions
     * - totalBudget: Total income (represents all money club has received)
     * - remaining: Same as balance
     */
    @GetMapping("/summary")
    public ApiResponse<FinanceSummaryResponse> getFinanceSummary(@PathVariable Long clubId) throws AppException {
        log.info("Fetching finance summary for club ID: {}", clubId);

        ClubWallet wallet = clubWalletService.getFinanceSummary(clubId);

        FinanceSummaryResponse response = FinanceSummaryResponse.builder()
                .balance(wallet.getBalance())
                .totalIncome(wallet.getTotalIncome())
                .totalExpense(wallet.getTotalOutcome())
                .totalBudget(wallet.getTotalIncome()) // Total budget = total income
                .remaining(wallet.getBalance())
                .currency(wallet.getCurrency())
                .walletId(wallet.getId())
                .build();

        return ApiResponse.success(response);
    }
}

