package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.PayOSConfigRequest;
import com.sep490.backendclubmanagement.dto.response.PayOSConfigResponse;
import com.sep490.backendclubmanagement.dto.request.PayOSCreatePaymentRequest;
import com.sep490.backendclubmanagement.dto.response.PayOSCreatePaymentResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.payment.PayOSIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/{clubId}/pay-os")
@RequiredArgsConstructor
public class PayOSController {

    private final PayOSIntegrationService payOSIntegrationService;

    @GetMapping("/config")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ResponseEntity<ApiResponse<PayOSConfigResponse>> getConfig(@PathVariable Long clubId) throws AppException {
        PayOSConfigResponse data = payOSIntegrationService.getConfig(clubId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/config")
    @PreAuthorize("@clubSecurity.isClubOfficerOrTreasureInClub(#clubId)")
    public ResponseEntity<ApiResponse<PayOSConfigResponse>> upsertConfig(
            @PathVariable Long clubId,
            @Valid @RequestBody PayOSConfigRequest request
    ) throws AppException {
        PayOSConfigResponse data = payOSIntegrationService.upsertConfig(clubId, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }



    @PostMapping("/create-payment")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ResponseEntity<ApiResponse<PayOSCreatePaymentResponse>> createPaymentRequest(
            @PathVariable Long clubId,
            @Valid @RequestBody PayOSCreatePaymentRequest request
    ) throws AppException {
        PayOSCreatePaymentResponse data = payOSIntegrationService.createPaymentRequest(clubId, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }


}


