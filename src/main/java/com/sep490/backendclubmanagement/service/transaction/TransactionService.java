package com.sep490.backendclubmanagement.service.transaction;

import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.TransactionResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final OutcomeTransactionRepository outcomeTransactionRepository;
    private final ClubWalletRepository clubWalletRepository;

    /**
     * Get all transactions (both income and outcome) for a club with pagination
     */
    public PageResponse<TransactionResponse> getAllTransactions(Long clubId, Pageable pageable) throws AppException {
        ClubWallet clubWallet = clubWalletRepository.findByClub_Id(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_WALLET_NOT_FOUND));

        // Get all income transactions
        List<IncomeTransaction> incomeTransactions = incomeTransactionRepository.findByClubWalletId(
                clubWallet.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Get all outcome transactions
        List<OutcomeTransaction> outcomeTransactions = outcomeTransactionRepository.findByClubWalletId(
                clubWallet.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Convert to combined TransactionResponse
        List<TransactionResponse> allTransactions = new ArrayList<>();

        // Add income transactions
        allTransactions.addAll(incomeTransactions.stream()
                .map(this::mapIncomeToTransactionResponse)
                .collect(Collectors.toList()));

        // Add outcome transactions
        allTransactions.addAll(outcomeTransactions.stream()
                .map(this::mapOutcomeToTransactionResponse)
                .collect(Collectors.toList()));

        // Sort by transaction date descending
        allTransactions.sort(Comparator.comparing(TransactionResponse::getTransactionDate).reversed());

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTransactions.size());
        List<TransactionResponse> pagedTransactions = allTransactions.subList(start, end);

        return PageResponse.<TransactionResponse>builder()
                .content(pagedTransactions)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements((long) allTransactions.size())
                .totalPages((int) Math.ceil((double) allTransactions.size() / pageable.getPageSize()))
                .hasNext(end < allTransactions.size())
                .hasPrevious(start > 0)
                .build();
    }

    /**
     * Get transactions by status
     */
    public PageResponse<TransactionResponse> getTransactionsByStatus(
            Long clubId, TransactionStatus status, Pageable pageable) throws AppException {
        ClubWallet clubWallet = clubWalletRepository.findByClub_Id(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_WALLET_NOT_FOUND));

        // Get income transactions with status
        List<IncomeTransaction> incomeTransactions = incomeTransactionRepository.findByClubWalletIdAndStatus(
                clubWallet.getId(),
                status,
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Get outcome transactions with status
        List<OutcomeTransaction> outcomeTransactions = outcomeTransactionRepository.findByClubWalletIdAndStatus(
                clubWallet.getId(),
                status,
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Convert to combined TransactionResponse
        List<TransactionResponse> allTransactions = new ArrayList<>();

        allTransactions.addAll(incomeTransactions.stream()
                .map(this::mapIncomeToTransactionResponse)
                .collect(Collectors.toList()));

        allTransactions.addAll(outcomeTransactions.stream()
                .map(this::mapOutcomeToTransactionResponse)
                .collect(Collectors.toList()));

        // Sort by transaction date descending
        allTransactions.sort(Comparator.comparing(TransactionResponse::getTransactionDate).reversed());

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTransactions.size());
        List<TransactionResponse> pagedTransactions = allTransactions.subList(start, end);

        return PageResponse.<TransactionResponse>builder()
                .content(pagedTransactions)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements((long) allTransactions.size())
                .totalPages((int) Math.ceil((double) allTransactions.size() / pageable.getPageSize()))
                .hasNext(end < allTransactions.size())
                .hasPrevious(start > 0)
                .build();
    }

    /**
     * Map IncomeTransaction to TransactionResponse
     */
    private TransactionResponse mapIncomeToTransactionResponse(IncomeTransaction income) {
        return TransactionResponse.builder()
                .id(income.getId())
                .code(income.getReference())
                .amount(income.getAmount())
                .description(income.getDescription())
                .transactionDate(income.getTransactionDate())
                .type(TransactionResponse.TransactionType.INCOME)
                .status(income.getStatus())
                .source(income.getSource())
                .feeId(income.getFee() != null ? income.getFee().getId() : null)
                .feeTitle(income.getFee() != null ? income.getFee().getTitle() : null)
                .notes(income.getNotes())
                .createdBy(income.getCreatedBy() != null ? income.getCreatedBy().getFullName() : null)
                .createdAt(income.getCreatedAt())
                .updatedAt(income.getUpdatedAt())
                .build();
    }

    /**
     * Map OutcomeTransaction to TransactionResponse
     */
    private TransactionResponse mapOutcomeToTransactionResponse(OutcomeTransaction outcome) {
        return TransactionResponse.builder()
                .id(outcome.getId())
                .code(outcome.getTransactionCode())
                .amount(outcome.getAmount())
                .description(outcome.getDescription())
                .transactionDate(outcome.getTransactionDate())
                .type(TransactionResponse.TransactionType.OUTCOME)
                .status(outcome.getStatus())
                .recipient(outcome.getRecipient())
                .purpose(outcome.getPurpose())
                .receiptUrl(outcome.getReceiptUrl())
                .notes(outcome.getNotes())
                .createdBy(outcome.getCreatedBy() != null ? outcome.getCreatedBy().getFullName() : null)
                .createdAt(outcome.getCreatedAt())
                .updatedAt(outcome.getUpdatedAt())
                .build();
    }
}

