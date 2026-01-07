package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.scheduled.WalletBalanceConsistencyCheckJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for wallet management operations
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletAdminController {

    private final WalletBalanceConsistencyCheckJob consistencyCheckJob;


    @PostMapping("/check-consistency")

    public ApiResponse<String> triggerConsistencyCheck() {
        log.info("Admin manually triggered wallet consistency check");

        try {
            consistencyCheckJob.manualCheckAndFix();
            return ApiResponse.success("Wallet consistency check completed successfully");

        } catch (Exception e) {
            log.error("Error during manual wallet consistency check: {}", e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .code(500)
                    .message("Error during consistency check: " + e.getMessage())
                    .timestamp(java.time.Instant.now())
                    .build();
        }
    }

    /**
     * Get current wallet consistency summary
     * Shows statistics: total wallets, consistent count, inconsistent count, etc.
     */
    @GetMapping("/consistency-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getConsistencySummary() {
        log.info("Admin requested wallet consistency summary");

        try {
            Map<String, Object> summary = consistencyCheckJob.getWalletConsistencySummary();
            return ApiResponse.success(summary);

        } catch (Exception e) {
            log.error("Error getting wallet consistency summary: {}", e.getMessage(), e);
            return ApiResponse.<Map<String, Object>>builder()
                    .code(500)
                    .message("Error getting consistency summary: " + e.getMessage())
                    .timestamp(java.time.Instant.now())
                    .build();
        }
    }
}

