package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import com.sep490.backendclubmanagement.scheduled.WalletBalanceConsistencyCheckJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for ClubWallet management and maintenance operations
 * Primarily for admin use
 */
@RestController
@RequestMapping("/api/v1/admin/club-wallets")
@RequiredArgsConstructor
@Slf4j
public class ClubWalletController {

    private final ClubWalletService clubWalletService;
    private final WalletBalanceConsistencyCheckJob walletBalanceConsistencyCheckJob;

    /**
     * Ensure all clubs have wallets
     * Manually trigger wallet creation for clubs without wallets
     */
    @PostMapping("/ensure-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ensureAllClubsHaveWallets() {
        log.info("Manual trigger: Ensuring all clubs have wallets");

        int walletsCreated = clubWalletService.ensureAllClubsHaveWallets();

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .code(200)
                .message("Đã kiểm tra và tạo ví cho các CLB")
                .data(Map.of(
                        "walletsCreated", walletsCreated,
                        "message", walletsCreated > 0
                                ? String.format("Đã tạo %d ví mới", walletsCreated)
                                : "Tất cả CLB đã có ví"
                ))
                .build());
    }

    /**
     * Get wallet consistency summary
     * Shows statistics about wallet balance consistency
     */
    @GetMapping("/consistency-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConsistencySummary() {
        log.info("Fetching wallet consistency summary");

        Map<String, Object> summary = walletBalanceConsistencyCheckJob.getWalletConsistencySummary();

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .code(200)
                .message("Thống kê tính nhất quán của ví CLB")
                .data(summary)
                .build());
    }

    /**
     * Manually trigger wallet balance consistency check and fix
     * Recalculates all wallet balances from transactions
     */
    @PostMapping("/check-and-fix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkAndFixWallets() {
        log.info("Manual trigger: Wallet balance consistency check and fix");

        walletBalanceConsistencyCheckJob.manualCheckAndFix();

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(200)
                .message("Đã thực hiện kiểm tra và sửa lỗi số dư ví")
                .data("Kiểm tra hoàn tất. Xem log để biết chi tiết.")
                .build());
    }

    /**
     * Get or create wallet for a specific club
     * Useful for testing or manual club setup
     */
    @PostMapping("/clubs/{clubId}/ensure-wallet")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ensureWalletForClub(@PathVariable Long clubId) {
        log.info("Manual trigger: Ensuring wallet for club ID: {}", clubId);

        try {
            var wallet = clubWalletService.getOrCreateWalletForClub(clubId);

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .code(200)
                    .message("Ví CLB đã sẵn sàng")
                    .data(Map.of(
                            "walletId", wallet.getId(),
                            "clubId", wallet.getClub().getId(),
                            "balance", wallet.getBalance(),
                            "totalIncome", wallet.getTotalIncome(),
                            "totalOutcome", wallet.getTotalOutcome(),
                            "currency", wallet.getCurrency()
                    ))
                    .build());
        } catch (Exception e) {
            log.error("Error ensuring wallet for club {}: {}", clubId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .code(400)
                    .message("Không thể tạo ví: " + e.getMessage())
                    .build());
        }
    }
}

