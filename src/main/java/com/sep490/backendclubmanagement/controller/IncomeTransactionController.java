package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.IncomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.TransactionStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService.UploadResult;
import com.sep490.backendclubmanagement.service.transaction.income.IncomeTransactionService;
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
 * Controller for managing Income Transactions
 * Handles all operations related to club income/revenue transactions
 */
@RestController
@RequestMapping("/api/clubs/{clubId}/transactions/income")
@RequiredArgsConstructor
public class IncomeTransactionController {

    private final IncomeTransactionService incomeTransactionServiceImpl;
    private final CloudinaryService cloudinaryService;

    /**
     * Get all income transactions for a club with filters
     * GET /api/clubs/{clubId}/transactions/income
     * 
     * @param search - Search in code, description, payer name (optional)
     * @param status - Filter by transaction status (optional)
     * @param fromDate - Filter from date (yyyy-MM-dd) (optional)
     * @param toDate - Filter to date (yyyy-MM-dd) (optional)
     * @param minAmount - Minimum amount filter (optional)
     * @param maxAmount - Maximum amount filter (optional)
     * @param source - Filter by income source: direct/bank/PayOS/other (optional)
     * @param feeId - Filter by fee ID (optional)
     */
    @GetMapping
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<IncomeTransactionResponse>> getIncomeTransactions(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long feeId
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

        PageResponse<IncomeTransactionResponse> response = 
                incomeTransactionServiceImpl.getIncomeTransactionsWithFilters(
                    clubId, search, status, parsedFromDate, parsedToDate, 
                    minAmount, maxAmount, source, feeId, pageable
                );

        return ApiResponse.success(response);
    }

    /**
     * Get income transaction by ID
     * GET /api/clubs/{clubId}/transactions/income/{transactionId}
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<IncomeTransactionResponse> getIncomeTransactionById(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        IncomeTransactionResponse response = incomeTransactionServiceImpl.getIncomeTransactionById(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Create a new income transaction
     * POST /api/clubs/{clubId}/transactions/income
     */
    @PostMapping
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<IncomeTransactionResponse> createIncomeTransaction(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateIncomeTransactionRequest request
    ) throws AppException {
        IncomeTransactionResponse response = incomeTransactionServiceImpl.createIncomeTransaction(clubId, request);
        return ApiResponse.success(response);
    }

    /**
     * Update income transaction (only PENDING status can be updated)
     * PUT /api/clubs/{clubId}/transactions/income/{transactionId}
     */
    @PutMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<IncomeTransactionResponse> updateIncomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateIncomeTransactionRequest request
    ) throws AppException {
        IncomeTransactionResponse response = incomeTransactionServiceImpl.updateIncomeTransaction(transactionId, request);
        return ApiResponse.success(response);
    }

    /**
     * Approve income transaction (PENDING -> SUCCESS)
     * POST /api/clubs/{clubId}/transactions/income/{transactionId}/approve
     */
    @PostMapping("/{transactionId}/approve")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<IncomeTransactionResponse> approveIncomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        IncomeTransactionResponse response = incomeTransactionServiceImpl.approveIncomeTransaction(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Reject income transaction (PENDING -> CANCELLED)
     * POST /api/clubs/{clubId}/transactions/income/{transactionId}/reject
     */
    @PostMapping("/{transactionId}/reject")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<IncomeTransactionResponse> rejectIncomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        IncomeTransactionResponse response = incomeTransactionServiceImpl.rejectIncomeTransaction(transactionId);
        return ApiResponse.success(response);
    }

    /**
     * Delete income transaction
     * DELETE /api/clubs/{clubId}/transactions/income/{transactionId}
     */
    @DeleteMapping("/{transactionId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<Void> deleteIncomeTransaction(
            @PathVariable Long clubId,
            @PathVariable Long transactionId
    ) throws AppException {
        incomeTransactionServiceImpl.deleteIncomeTransaction(transactionId);
        return ApiResponse.success();
    }

    /**
     * Upload receipt image for income transaction
     * POST /api/clubs/{clubId}/transactions/income/upload-receipt
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

            // Upload to Cloudinary in club/transactions/income/receipts folder
            UploadResult result = cloudinaryService.uploadImage(file, "club/transactions/income/receipts");

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

