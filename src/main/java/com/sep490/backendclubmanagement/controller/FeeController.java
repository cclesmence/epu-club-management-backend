package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateFeeRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateFeeRequest;
import com.sep490.backendclubmanagement.dto.response.FeeDetailResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.PayOSCreatePaymentResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.fee.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/fees")
@RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;

    @GetMapping
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<FeeDetailResponse>> getFees(
            @PathVariable Long clubId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isExpired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt")
        ));
        PageResponse<FeeDetailResponse> responses;
        if ((search != null && !search.trim().isEmpty()) || isExpired != null) {
            responses = feeService.searchFees(clubId, search, isExpired, pageable);
        } else {
            responses = feeService.getFeesByClubId(clubId, pageable);
        }
        return ApiResponse.success(responses);
    }

    @GetMapping("/drafts")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId) or @clubSecurity.isTreasureInClub(#clubId)")
    public ApiResponse<List<FeeDetailResponse>> getDraftFees(@PathVariable Long clubId) {
        List<FeeDetailResponse> responses = feeService.getDraftFeesByClubId(clubId);
        return ApiResponse.success(responses);
    }

    @GetMapping("/check-title")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<Boolean> checkFeeTitleExists(
            @PathVariable Long clubId,
            @RequestParam String title,
            @RequestParam(required = false) Long excludeFeeId
    ) {
        boolean exists;
        if (excludeFeeId != null) {
            exists = feeService.isFeeTitleExistsExcluding(clubId, title, excludeFeeId);
        } else {
            exists = feeService.isFeeTitleExists(clubId, title);
        }
        return ApiResponse.success(exists);
    }

    @PostMapping
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId) or @clubSecurity.isTreasureInClub(#clubId)")
    public ApiResponse<FeeDetailResponse> createFee(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateFeeRequest request) {
        try {
            FeeDetailResponse feeDto = feeService.createFee(clubId, request);
            return ApiResponse.success(feeDto);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        }
    }

    @PutMapping("/{feeId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<FeeDetailResponse> updateFee(
            @PathVariable Long clubId,
            @PathVariable Long feeId,
            @Valid @RequestBody UpdateFeeRequest request) {
        try {
            FeeDetailResponse feeDto = feeService.updateFee(feeId, request);
            return ApiResponse.success(feeDto);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        }
    }

    @DeleteMapping("/{feeId}")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<Void> deleteFee(
            @PathVariable Long clubId,
            @PathVariable Long feeId) {
        try {
            feeService.deleteFee(feeId);
            return ApiResponse.success(null);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        }
    }

    @PostMapping("/{feeId}/generate-payment")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PayOSCreatePaymentResponse> generatePaymentQR(
            @PathVariable Long clubId,
            @PathVariable Long feeId,
            @RequestParam Long userId
    ) {
        try {
            PayOSCreatePaymentResponse response = feeService.generatePaymentQR(clubId, feeId, userId);
            return ApiResponse.success(response);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Không thể tạo mã QR thanh toán: " + Arrays.toString(ex.getStackTrace()), null);
        }
    }

    @PatchMapping("/{feeId}/publish")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ApiResponse<FeeDetailResponse> publishFee(
            @PathVariable Long clubId,
            @PathVariable Long feeId
    ) {
        try {
            FeeDetailResponse feeDto = feeService.publishFee(feeId);
            return ApiResponse.success(feeDto);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        }
    }

    @GetMapping("/{feeId}/paid-members")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<com.sep490.backendclubmanagement.dto.response.FeePaidMemberResponse>> getPaidMembers(
            @PathVariable Long clubId,
            @PathVariable Long feeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            PageResponse<com.sep490.backendclubmanagement.dto.response.FeePaidMemberResponse> response =
                    feeService.getPaidMembersByFee(feeId, search, pageable);
            return ApiResponse.success(response);
        } catch (AppException ex) {
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null);
        }
    }

    @GetMapping("/unpaid")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<List<FeeDetailResponse>> getUnpaidFees(
            @PathVariable Long clubId,
            @RequestParam Long userId
    ) {
        List<FeeDetailResponse> unpaidFees = feeService.getUnpaidFeesByUser(clubId, userId);
        return ApiResponse.success(unpaidFees);
    }

    @GetMapping("/paid")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<FeeDetailResponse>> getPaidFees(
            @PathVariable Long clubId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<FeeDetailResponse> paidFees = feeService.getPaidFeesByUser(clubId, userId, pageable);
        return ApiResponse.success(paidFees);
    }

}
