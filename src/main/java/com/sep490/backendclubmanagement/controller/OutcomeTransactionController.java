package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.OutcomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.TransactionStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService.UploadResult;
import com.sep490.backendclubmanagement.service.transaction.outcome.OutcomeTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller for managing Outcome Transactions
 * Handles all operations related to club expense/spending transactions
 */
@RestController
@RequestMapping("/api/clubs/{clubId}/transactions/outcome")
@RequiredArgsConstructor
public class OutcomeTransactionController {

    private final OutcomeTransactionService outcomeTransactionServiceImpl;
    private final CloudinaryService cloudinaryService;

    /**
     * Get all outcome transactions for a club with filters
     * GET /api/clubs/{clubId}/transactions/outcome
     * 
     * @param search - Search in code, description, recipient name (optional)
     * @param status - Filter by transaction status (optional)
     * @param fromDate - Filter from date (yyyy-MM-dd) (optional)
     * @param toDate - Filter to date (yyyy-MM-dd) (optional)
     * @param minAmount - Minimum amount filter (optional)
     * @param maxAmount - Maximum amount filter (optional)
     * @param category - Filter by outcome category (optional)
     */
    @GetMapping
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<OutcomeTransactionResponse>> getOutcomeTransactions(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(required = false) String category
    ) throws AppException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate")));

        // Parse dates if provided
        java.time.LocalDate parsedFromDate = null;
        java.time.LocalDate parsedToDate = null;
        
        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                parsedFromDate = java.time.LocalDate.parse(fromDate);
            } catch (Exception e) {
                return ApiResponse.error(400, "Invalid fromDate format. Use yyyy-MM-dd");
            }
        }
        
        if (toDate != null && !toDate.isEmpty()) {
            try {
                parsedToDate = java.time.LocalDate.parse(toDate);
            } catch (Exception e) {
                return ApiResponse.error(400, "Invalid toDate format. Use yyyy-MM-dd");
            }
        }

        PageResponse<OutcomeTransactionResponse> response = 
                outcomeTransactionServiceImpl.getOutcomeTransactionsWithFilters(
                    clubId, search, status, parsedFromDate, parsedToDate, 
                    minAmount, maxAmount, category, pageable
                );

        return ApiResponse.success(response);
    }

    /**
     * Get outcome transaction by ID
     * GET /api/clubs/{clubId}/transactions/outcome/{transactionId}
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<OutcomeTransactionResponse> getOutcomeTransactionById(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        OutcomeTransactionResponse response = outcomeTransactionServiceImpl.getOutcomeTransactionById(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Create a new outcome transaction
     * POST /api/clubs/{clubId}/transactions/outcome
     */
    @PostMapping
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<OutcomeTransactionResponse> createOutcomeTransaction(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateOutcomeTransactionRequest request
    ) throws AppException {
        OutcomeTransactionResponse response = outcomeTransactionServiceImpl.createOutcomeTransaction(clubId, request);
        return ApiResponse.success(response);
    }

    /**
     * Update outcome transaction (only PENDING status can be updated)
     * PUT /api/clubs/{clubId}/transactions/outcome/{transactionId}
     */
    @PutMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<OutcomeTransactionResponse> updateOutcomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateOutcomeTransactionRequest request
    ) throws AppException {
        OutcomeTransactionResponse response = outcomeTransactionServiceImpl.updateOutcomeTransaction(transactionId, request);
        return ApiResponse.success(response);
    }

    /**
     * Approve outcome transaction (PENDING -> SUCCESS)
     * POST /api/clubs/{clubId}/transactions/outcome/{transactionId}/approve
     */
    @PostMapping("/{transactionId}/approve")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<OutcomeTransactionResponse> approveOutcomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        OutcomeTransactionResponse response = outcomeTransactionServiceImpl.approveOutcomeTransaction(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Reject outcome transaction (PENDING -> CANCELLED)
     * POST /api/clubs/{clubId}/transactions/outcome/{transactionId}/reject
     */
    @PostMapping("/{transactionId}/reject")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<OutcomeTransactionResponse> rejectOutcomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        OutcomeTransactionResponse response = outcomeTransactionServiceImpl.rejectOutcomeTransaction(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Delete outcome transaction
     * DELETE /api/clubs/{clubId}/transactions/outcome/{transactionId}
     */
    @DeleteMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<Void> deleteOutcomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        outcomeTransactionServiceImpl.deleteOutcomeTransaction(transactionId);
        return ApiResponse.success();
    }

    /**
     * Upload receipt image for outcome transaction
     * POST /api/clubs/{clubId}/transactions/outcome/upload-receipt
     *
     * Uploads image to Cloudinary and returns the URL
     * Frontend can then include this URL when creating/updating transaction
     */
    @PostMapping("/upload-receipt")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<Map<String, String>> uploadReceiptImage(
            @PathVariable Long clubId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ApiResponse.error(400, "File không được để trống");
            }

            // Validate file type (only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ApiResponse.error(400, "Chỉ chấp nhận file ảnh (jpg, png, gif, etc.)");
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ApiResponse.error(400, "Kích thước file không được vượt quá 5MB");
            }

            // Upload to Cloudinary in club/transactions/outcome/receipts folder
            UploadResult result = cloudinaryService.uploadImage(file, "club/transactions/outcome/receipts");

            return ApiResponse.success(Map.of(
                    "receiptUrl", result.url(),
                    "publicId", result.publicId(),
                    "message", "Upload ảnh bằng chứng thành công"
            ));
        } catch (Exception e) {
            return ApiResponse.error(500, "Lỗi khi upload ảnh: " + e.getMessage());
        }
    }
}

