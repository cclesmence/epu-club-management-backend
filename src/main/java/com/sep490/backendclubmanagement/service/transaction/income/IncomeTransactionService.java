package com.sep490.backendclubmanagement.service.transaction.income;

import com.sep490.backendclubmanagement.dto.request.CreateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.IncomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.TransactionStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IncomeTransactionService {

    /**
     * Get all income transactions for a club with pagination
     */
    PageResponse<IncomeTransactionResponse> getIncomeTransactions(Long clubId, Pageable pageable) throws AppException;

    /**
     * Get income transactions by status
     */
    PageResponse<IncomeTransactionResponse> getIncomeTransactionsByStatus(
            Long clubId, TransactionStatus status, Pageable pageable) throws AppException;

    /**
     * Get income transactions with filters
     */
    PageResponse<IncomeTransactionResponse> getIncomeTransactionsWithFilters(
            Long clubId,
            String search,
            TransactionStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String source,
            Long feeId,
            Pageable pageable) throws AppException;

    /**
     * Get income transaction by ID
     */
    IncomeTransactionResponse getIncomeTransactionById(Long transactionId) throws AppException;

    /**
     * Create a new income transaction
     * - If user has CLUB_OFFICER role: status = SUCCESS (auto-approved)
     * - If user has CLUB_TREASURER role: status = PENDING (needs approval)
     */
    IncomeTransactionResponse createIncomeTransaction(Long clubId, CreateIncomeTransactionRequest request) throws AppException;

    /**
     * Update income transaction (only PENDING status can be updated)
     */
    IncomeTransactionResponse updateIncomeTransaction(Long transactionId, UpdateIncomeTransactionRequest request) throws AppException;

    /**
     * Approve income transaction (PENDING -> SUCCESS)
     * This will update the club wallet balance
     */
    IncomeTransactionResponse approveIncomeTransaction(Long transactionId) throws AppException;

    /**
     * Reject income transaction (PENDING -> CANCELLED)
     */
    IncomeTransactionResponse rejectIncomeTransaction(Long transactionId) throws AppException;

    /**
     * Delete income transaction (only if PENDING or CANCELLED)
     */
    void deleteIncomeTransaction(Long transactionId) throws AppException;
}

