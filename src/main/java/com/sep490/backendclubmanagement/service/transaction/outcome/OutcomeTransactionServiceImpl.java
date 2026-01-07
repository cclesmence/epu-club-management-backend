package com.sep490.backendclubmanagement.service.transaction.outcome;

import com.sep490.backendclubmanagement.dto.request.CreateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.OutcomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.OutcomeTransactionMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutcomeTransactionServiceImpl implements OutcomeTransactionService {

    private final OutcomeTransactionRepository outcomeTransactionRepository;
    private final ClubWalletRepository clubWalletRepository;
    private final UserRepository userRepository;
    private final OutcomeTransactionMapper outcomeTransactionMapper;
    private final UserService userService;
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final ClubWalletService clubWalletService;

    /**
     * Get all outcome transactions for a club with pagination
     */
    public PageResponse<OutcomeTransactionResponse> getOutcomeTransactions(Long clubId, Pageable pageable) throws AppException {
        // Auto-create wallet if not exists
        ClubWallet clubWallet = clubWalletService.getOrCreateWalletForClub(clubId);

        Page<OutcomeTransaction> page = outcomeTransactionRepository.findByClubWalletId(clubWallet.getId(), pageable);

        List<OutcomeTransactionResponse> content = page.getContent().stream()
                .map(outcomeTransactionMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<OutcomeTransactionResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Get outcome transactions by status
     */
    public PageResponse<OutcomeTransactionResponse> getOutcomeTransactionsByStatus(
            Long clubId, TransactionStatus status, Pageable pageable) throws AppException {
        // Auto-create wallet if not exists
        ClubWallet clubWallet = clubWalletService.getOrCreateWalletForClub(clubId);

        Page<OutcomeTransaction> page = outcomeTransactionRepository.findByClubWalletIdAndStatus(
                clubWallet.getId(), status, pageable);

        List<OutcomeTransactionResponse> content = page.getContent().stream()
                .map(outcomeTransactionMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<OutcomeTransactionResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Get outcome transactions with filters
     * Search uses Vietnamese accent-insensitive matching (VietnameseTextNormalizer)
     */
    @Override
    public PageResponse<OutcomeTransactionResponse> getOutcomeTransactionsWithFilters(
            Long clubId,
            String search,
            TransactionStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String category,
            Pageable pageable) throws AppException {
        
        // Auto-create wallet if not exists
        ClubWallet clubWallet = clubWalletService.getOrCreateWalletForClub(clubId);

        // Normalize search term for Vietnamese accent-insensitive search
        final String normalizedSearch = (search != null && !search.trim().isEmpty()) 
                ? search.trim() 
                : null;

        // Build dynamic query specification (without text search)
        Specification<OutcomeTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            // Club wallet filter (required)
            predicates.add(cb.equal(root.get("clubWallet").get("id"), clubWallet.getId()));

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Date range filter
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), toDate));
            }

            // Amount range filter
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // Category filter
            if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OutcomeTransaction> page = outcomeTransactionRepository.findAll(spec, pageable);

        // 🔍 Apply Vietnamese accent-insensitive search filter in Java
        List<OutcomeTransaction> filteredTransactions = page.getContent();
        if (normalizedSearch != null) {
            filteredTransactions = filteredTransactions.stream()
                    .filter(transaction -> {
                        // Build searchable fields
                        String transactionCode = transaction.getTransactionCode() != null ? transaction.getTransactionCode() : "";
                        String description = transaction.getDescription() != null ? transaction.getDescription() : "";
                        String recipient = transaction.getRecipient() != null ? transaction.getRecipient() : "";
                        
                        // Use VietnameseTextNormalizer for accent-insensitive search
                        return com.sep490.backendclubmanagement.util.VietnameseTextNormalizer.matchesAny(
                                normalizedSearch,
                                transactionCode,
                                description,
                                recipient
                        );
                    })
                    .collect(Collectors.toList());
        }

        List<OutcomeTransactionResponse> content = filteredTransactions.stream()
                .map(outcomeTransactionMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<OutcomeTransactionResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Get outcome transaction by ID
     */
    public OutcomeTransactionResponse getOutcomeTransactionById(Long transactionId) throws AppException {
        OutcomeTransaction transaction = outcomeTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        return outcomeTransactionMapper.toResponse(transaction);
    }

    /**
     * Create a new outcome transaction
     * - If user has CLUB_OFFICER role: status = SUCCESS (auto-approved) and update wallet immediately
     * - Otherwise (CLUB_TREASURER or other): status = PENDING (needs approval)
     */
    @Transactional
    public OutcomeTransactionResponse createOutcomeTransaction(Long clubId, CreateOutcomeTransactionRequest request) throws AppException {
        // Auto-create wallet if not exists
        ClubWallet clubWallet = clubWalletService.getOrCreateWalletForClub(clubId);

        // Get current user
        Long currentUserId = userService.getCurrentUserId();
        User createdBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check user's role in the club to determine transaction status
        // existsClubAdmin checks if user has roleLevel <= 2 (CLUB_OFFICER) in current semester
        boolean isClubOfficer = roleMemberShipRepository.existsClubAdmin(currentUserId, clubId);
        TransactionStatus initialStatus = isClubOfficer ? TransactionStatus.SUCCESS : TransactionStatus.PENDING;

        // Generate unique transaction code
        String transactionCode = generateUniqueTransactionCode("OUT");

        // Build outcome transaction
        OutcomeTransaction transaction = OutcomeTransaction.builder()
                .transactionCode(transactionCode)
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .recipient(request.getRecipient())
                .purpose(request.getPurpose())
                .status(initialStatus)
                .notes(request.getNotes())
                .receiptUrl(request.getReceiptUrl())
                .clubWallet(clubWallet)
                .createdBy(createdBy)
                .build();

        OutcomeTransaction savedTransaction = outcomeTransactionRepository.save(transaction);

        // Process wallet update (TiDB doesn't support triggers - handle in application)
        // This will check balance and throw exception if insufficient
        clubWalletService.processOutcomeTransaction(savedTransaction, null);

        if (isClubOfficer) {
            log.info("Created and auto-approved outcome transaction: {} for club: {} by CLUB_OFFICER. Wallet updated.", transactionCode, clubId);
        } else {
            log.info("Created outcome transaction: {} for club: {} with PENDING status", transactionCode, clubId);
        }

        return outcomeTransactionMapper.toResponse(savedTransaction);
    }

    /**
     * Update outcome transaction
     */
    @Transactional
    public OutcomeTransactionResponse updateOutcomeTransaction(Long transactionId, UpdateOutcomeTransactionRequest request) throws AppException {
        OutcomeTransaction transaction = outcomeTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Only allow update if status is PENDING
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSACTION_CANNOT_BE_UPDATED);
        }

        // Update fields
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setRecipient(request.getRecipient());
        transaction.setPurpose(request.getPurpose());
        transaction.setNotes(request.getNotes());
        transaction.setReceiptUrl(request.getReceiptUrl());

        OutcomeTransaction updatedTransaction = outcomeTransactionRepository.save(transaction);
        log.info("Updated outcome transaction: {}", transactionId);

        return outcomeTransactionMapper.toResponse(updatedTransaction);
    }

    /**
     * Approve outcome transaction (PENDING -> SUCCESS)
     * This will update the club wallet balance by deducting the amount
     */
    @Transactional
    public OutcomeTransactionResponse approveOutcomeTransaction(Long transactionId) throws AppException {
        OutcomeTransaction transaction = outcomeTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSACTION_ALREADY_PROCESSED);
        }

        // Store old state for wallet processing
        OutcomeTransaction oldTransaction = OutcomeTransaction.builder()
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .clubWallet(transaction.getClubWallet())
                .build();

        // Update transaction status to SUCCESS
        transaction.setStatus(TransactionStatus.SUCCESS);
        OutcomeTransaction approvedTransaction = outcomeTransactionRepository.save(transaction);

        // Process wallet update (TiDB doesn't support triggers - handle in application)
        // This will check balance and throw exception if insufficient
        clubWalletService.processOutcomeTransaction(approvedTransaction, oldTransaction);

        log.info("Approved outcome transaction: {}, amount: {}. Wallet updated.", transactionId, transaction.getAmount());

        return outcomeTransactionMapper.toResponse(approvedTransaction);
    }

    /**
     * Reject outcome transaction (PENDING -> CANCELLED)
     */
    @Transactional
    public OutcomeTransactionResponse rejectOutcomeTransaction(Long transactionId) throws AppException {
        OutcomeTransaction transaction = outcomeTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSACTION_ALREADY_PROCESSED);
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        OutcomeTransaction rejectedTransaction = outcomeTransactionRepository.save(transaction);

        log.info("Rejected outcome transaction: {}", transactionId);

        return outcomeTransactionMapper.toResponse(rejectedTransaction);
    }

    /**
     * Delete outcome transaction (only if PENDING or CANCELLED)
     */
    @Transactional
    public void deleteOutcomeTransaction(Long transactionId) throws AppException {
        OutcomeTransaction transaction = outcomeTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Can only delete PENDING or CANCELLED transactions
        if (transaction.getStatus() == TransactionStatus.SUCCESS ||
            transaction.getStatus() == TransactionStatus.PROCESSING) {
            throw new AppException(ErrorCode.TRANSACTION_CANNOT_BE_DELETED);
        }

        outcomeTransactionRepository.delete(transaction);
        log.info("Deleted outcome transaction: {}", transactionId);
    }

    /**
     * Generate unique transaction code
     */
    private String generateUniqueTransactionCode(String prefix) {
        String transactionCode;
        do {
            transactionCode = prefix + "-" + LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" +
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (outcomeTransactionRepository.existsByTransactionCode(transactionCode));

        return transactionCode;
    }

}

