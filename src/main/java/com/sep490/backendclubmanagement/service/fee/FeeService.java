package com.sep490.backendclubmanagement.service.fee;

import com.sep490.backendclubmanagement.dto.request.CreateFeeRequest;
import com.sep490.backendclubmanagement.dto.request.PayOSWebhookRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateFeeRequest;
import com.sep490.backendclubmanagement.dto.response.FeeDetailResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.PayOSCreatePaymentResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface FeeService {

    PageResponse<FeeDetailResponse> getFeesByClubId(Long clubId, Pageable pageable);

    PageResponse<FeeDetailResponse> searchFees(Long clubId, String searchTerm, Boolean isExpired, Pageable pageable);

    List<FeeDetailResponse> getPaidFeesByUser(Long clubId, Long userId);

    PageResponse<FeeDetailResponse> getPaidFeesByUser(Long clubId, Long userId, Pageable pageable);

    FeeDetailResponse createFee(Long clubId, CreateFeeRequest request) throws AppException;
    boolean isFeeTitleExists(Long clubId, String title);

    boolean isFeeTitleExistsExcluding(Long clubId, String title, Long excludeFeeId);

    FeeDetailResponse updateFee(Long feeId, UpdateFeeRequest request) throws AppException;

    List<FeeDetailResponse> getDraftFeesByClubId(Long clubId);

    FeeDetailResponse publishFee(Long feeId) throws AppException;

    void deleteFee(Long feeId) throws AppException;

    PayOSCreatePaymentResponse generatePaymentQR(Long clubId, Long feeId, Long userId) throws AppException;

    void handlePaymentWebhook(PayOSWebhookRequest webhookRequest) throws AppException;

    List<FeeDetailResponse> getUnpaidFeesByUser(Long clubId, Long userId);

    PageResponse<com.sep490.backendclubmanagement.dto.response.FeePaidMemberResponse> getPaidMembersByFee(Long feeId, String searchTerm, Pageable pageable) throws AppException;
}

