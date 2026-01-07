package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.TransactionResponse;
import com.sep490.backendclubmanagement.entity.TransactionStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for getting combined transactions (both income and outcome)
 * Useful for displaying all transactions together in the UI
 */
@RestController
@RequestMapping("/api/clubs/{clubId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Get all transactions (both income and outcome) for a club
     * GET /api/clubs/{clubId}/transactions
     */
    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> getAllTransactions(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TransactionStatus status
    ) throws AppException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate")));

        PageResponse<TransactionResponse> response;
        if (status != null) {
            response = transactionService.getTransactionsByStatus(clubId, status, pageable);
        } else {
            response = transactionService.getAllTransactions(clubId, pageable);
        }

        return ApiResponse.success(response);
    }
}

