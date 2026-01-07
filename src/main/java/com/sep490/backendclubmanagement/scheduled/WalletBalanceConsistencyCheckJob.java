package com.sep490.backendclubmanagement.scheduled;

import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Scheduled job to check and fix ClubWallet balance inconsistencies
 * Also ensures all clubs have wallets on startup
 * Runs daily at 2:00 AM as a safety net
 * With database triggers in place, inconsistencies should be rare or zero
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletBalanceConsistencyCheckJob {

    private final JdbcTemplate jdbcTemplate;
    private final ClubWalletService clubWalletService;

    /**
     * Run once when application starts
     * Ensures all clubs have wallets and checks balance consistency
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("=== Application ready - Running wallet initialization check ===");

        try {
            // Step 1: Ensure all clubs have wallets
            int walletsCreated = clubWalletService.ensureAllClubsHaveWallets();
            if (walletsCreated > 0) {
                log.info("✅ Created {} new wallet(s) on startup", walletsCreated);
            }

            // Step 2: Check wallet balance consistency
            log.info("Running initial wallet balance consistency check...");
            checkAndFixWalletBalance();

        } catch (Exception e) {
            log.error("❌ Error during wallet initialization on startup: {}", e.getMessage(), e);
        }

        log.info("=== Completed wallet initialization check ===");
    }

    /**
     * Daily check and fix wallet balance inconsistencies
     * Schedule: Every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    @Transactional
    public void checkAndFixWalletBalance() {
        long startTime = System.currentTimeMillis();
        log.info("=== Starting daily wallet balance consistency check ===");

        try {
            // Step 0a: Ensure all clubs have wallets first
            log.info("Checking for clubs without wallets...");
            int walletsCreated = clubWalletService.ensureAllClubsHaveWallets();
            if (walletsCreated > 0) {
                log.info("✅ Created {} missing wallet(s) during scheduled check", walletsCreated);
            }

            // Step 0b: Get total wallet count
            int totalWallets = getTotalWalletCount();
            log.info("📊 Total wallets in system: {}", totalWallets);

            if (totalWallets == 0) {
                log.info("ℹ️ No wallets found in system. Skipping check.");
                return;
            }

            // Step 1: Check for inconsistencies
            List<Map<String, Object>> inconsistentWallets = findInconsistentWallets();

            if (inconsistentWallets.isEmpty()) {
                log.info("✅ All {} wallet balances are consistent. No action needed.", totalWallets);
                log.info("📈 Consistency rate: 100%");
                return;
            }

            // Step 2: Log inconsistent wallets with details
            double inconsistencyRate = (inconsistentWallets.size() * 100.0) / totalWallets;
            log.warn("⚠️ Found {} inconsistent wallet(s) out of {} total ({} %)",
                    inconsistentWallets.size(), totalWallets, String.format("%.2f", inconsistencyRate));

            for (Map<String, Object> wallet : inconsistentWallets) {
                log.warn("  📌 Club ID: {}, Wallet ID: {}, Current Balance: {}, Expected Balance: {}, Difference: {}, " +
                        "Total Income (Current/Expected): {}/{}, Total Outcome (Current/Expected): {}/{}",
                        wallet.get("club_id"),
                        wallet.get("id"),
                        wallet.get("current_balance"),
                        wallet.get("calculated_balance"),
                        wallet.get("difference"),
                        wallet.get("current_total_income"),
                        wallet.get("actual_income"),
                        wallet.get("current_total_outcome"),
                        wallet.get("actual_outcome"));
            }

            // Step 3: Fix inconsistencies
            log.info("🔧 Attempting to fix {} inconsistent wallet(s)...", inconsistentWallets.size());
            int fixedCount = fixInconsistentWallets();
            log.info("✅ Fixed {} wallet balance(s)", fixedCount);

            // Step 4: Verify after fix
            List<Map<String, Object>> remainingInconsistencies = findInconsistentWallets();
            if (!remainingInconsistencies.isEmpty()) {
                log.error("❌ Still have {} inconsistent wallet(s) after fix! Manual investigation required.",
                        remainingInconsistencies.size());
                log.error("💡 Problematic wallets:");
                for (Map<String, Object> wallet : remainingInconsistencies) {
                    log.error("  - Wallet ID: {}, Club ID: {}, Difference: {}",
                            wallet.get("id"), wallet.get("club_id"), wallet.get("difference"));
                }
                // TODO: Send alert/notification to admin
            } else {
                log.info("✅ All wallet balances are now consistent after fix.");
                log.info("📈 Final consistency rate: 100%");
            }

        } catch (Exception e) {
            log.error("❌ Error during wallet balance consistency check: {}", e.getMessage(), e);
            log.error("Stack trace:", e);
            // TODO: Send error alert/notification to admin
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=== Completed wallet balance consistency check (took {}ms) ===", duration);
    }

    /**
     * Get total number of wallets in system
     */
    private int getTotalWalletCount() {
        try {
            String query = "SELECT COUNT(*) FROM club_wallets";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error getting total wallet count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Find wallets with inconsistent balances
     * Uses tolerance of 0.01 for floating point comparison
     */
    private List<Map<String, Object>> findInconsistentWallets() {
        String query = """
                SELECT
                    cw.id,
                    cw.club_id,
                    COALESCE(cw.balance, 0) AS current_balance,
                    COALESCE(cw.total_income, 0) AS current_total_income,
                    COALESCE(cw.total_outcome, 0) AS current_total_outcome,
                    COALESCE(SUM(CASE WHEN it.status = 'SUCCESS' AND (it.deleted_at IS NULL OR it.deleted_at > NOW()) 
                                      THEN it.amount ELSE 0 END), 0) AS actual_income,
                    COALESCE(SUM(CASE WHEN ot.status = 'SUCCESS' AND (ot.deleted_at IS NULL OR ot.deleted_at > NOW()) 
                                      THEN ot.amount ELSE 0 END), 0) AS actual_outcome,
                    (COALESCE(SUM(CASE WHEN it.status = 'SUCCESS' AND (it.deleted_at IS NULL OR it.deleted_at > NOW()) 
                                       THEN it.amount ELSE 0 END), 0) -
                     COALESCE(SUM(CASE WHEN ot.status = 'SUCCESS' AND (ot.deleted_at IS NULL OR ot.deleted_at > NOW()) 
                                       THEN ot.amount ELSE 0 END), 0)) AS calculated_balance,
                    (COALESCE(cw.balance, 0) - 
                     (COALESCE(SUM(CASE WHEN it.status = 'SUCCESS' AND (it.deleted_at IS NULL OR it.deleted_at > NOW()) 
                                        THEN it.amount ELSE 0 END), 0) -
                      COALESCE(SUM(CASE WHEN ot.status = 'SUCCESS' AND (ot.deleted_at IS NULL OR ot.deleted_at > NOW()) 
                                        THEN ot.amount ELSE 0 END), 0))) AS difference
                FROM club_wallets cw
                LEFT JOIN income_transactions it ON it.club_wallet_id = cw.id
                LEFT JOIN outcome_transactions ot ON ot.club_wallet_id = cw.id
                GROUP BY cw.id, cw.club_id, cw.balance, cw.total_income, cw.total_outcome
                HAVING ABS(COALESCE(cw.balance, 0) - calculated_balance) > 0.01
                    OR ABS(COALESCE(cw.total_income, 0) - actual_income) > 0.01
                    OR ABS(COALESCE(cw.total_outcome, 0) - actual_outcome) > 0.01
                ORDER BY ABS(difference) DESC
                """;

        try {
            return jdbcTemplate.queryForList(query);
        } catch (Exception e) {
            log.error("Error finding inconsistent wallets: {}", e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Fix inconsistent wallet balances by recalculating from transactions
     * Returns number of wallets fixed
     */
    private int fixInconsistentWallets() {
        String updateQuery = """
                UPDATE club_wallets cw
                SET
                    total_income = (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM income_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    ),
                    total_outcome = (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM outcome_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    ),
                    balance = (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM income_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    ) - (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM outcome_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    ),
                    updated_at = NOW()
                WHERE
                    ABS(COALESCE(cw.balance, 0) - (
                        (SELECT COALESCE(SUM(amount), 0)
                         FROM income_transactions
                         WHERE club_wallet_id = cw.id 
                           AND status = 'SUCCESS'
                           AND (deleted_at IS NULL OR deleted_at > NOW())) -
                        (SELECT COALESCE(SUM(amount), 0)
                         FROM outcome_transactions
                         WHERE club_wallet_id = cw.id 
                           AND status = 'SUCCESS'
                           AND (deleted_at IS NULL OR deleted_at > NOW()))
                    )) > 0.01
                    OR ABS(COALESCE(cw.total_income, 0) - (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM income_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    )) > 0.01
                    OR ABS(COALESCE(cw.total_outcome, 0) - (
                        SELECT COALESCE(SUM(amount), 0)
                        FROM outcome_transactions
                        WHERE club_wallet_id = cw.id 
                          AND status = 'SUCCESS'
                          AND (deleted_at IS NULL OR deleted_at > NOW())
                    )) > 0.01
                """;

        try {
            int updatedCount = jdbcTemplate.update(updateQuery);
            log.info("📝 Updated {} wallet record(s) in database", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("❌ Error fixing inconsistent wallets: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get summary statistics of wallet consistency
     * Useful for monitoring dashboard
     */
    public Map<String, Object> getWalletConsistencySummary() {
        String query = """
                SELECT
                    COUNT(*) as total_wallets,
                    SUM(CASE WHEN status = 'OK' THEN 1 ELSE 0 END) as consistent_wallets,
                    SUM(CASE WHEN status = 'INCONSISTENT' THEN 1 ELSE 0 END) as inconsistent_wallets,
                    CASE 
                        WHEN COUNT(*) > 0 THEN CONCAT(ROUND(SUM(CASE WHEN status = 'OK' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2), '%')
                        ELSE '0%'
                    END as consistency_rate
                FROM (
                    SELECT
                        cw.id,
                        CASE
                            WHEN ABS(COALESCE(cw.balance, 0) - (
                                    COALESCE((SELECT SUM(amount) FROM income_transactions 
                                             WHERE club_wallet_id = cw.id 
                                               AND status = 'SUCCESS' 
                                               AND (deleted_at IS NULL OR deleted_at > NOW())), 0) -
                                    COALESCE((SELECT SUM(amount) FROM outcome_transactions 
                                             WHERE club_wallet_id = cw.id 
                                               AND status = 'SUCCESS' 
                                               AND (deleted_at IS NULL OR deleted_at > NOW())), 0)
                                )) <= 0.01
                                AND ABS(COALESCE(cw.total_income, 0) - COALESCE((SELECT SUM(amount) FROM income_transactions 
                                                               WHERE club_wallet_id = cw.id 
                                                                 AND status = 'SUCCESS' 
                                                                 AND (deleted_at IS NULL OR deleted_at > NOW())), 0)) <= 0.01
                                AND ABS(COALESCE(cw.total_outcome, 0) - COALESCE((SELECT SUM(amount) FROM outcome_transactions 
                                                                WHERE club_wallet_id = cw.id 
                                                                  AND status = 'SUCCESS' 
                                                                  AND (deleted_at IS NULL OR deleted_at > NOW())), 0)) <= 0.01
                            THEN 'OK'
                            ELSE 'INCONSISTENT'
                        END AS status
                    FROM club_wallets cw
                ) AS wallet_status
                """;

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query);
            log.debug("Wallet consistency summary: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error getting wallet consistency summary: {}", e.getMessage(), e);
            // Return default values on error
            return Map.of(
                "total_wallets", 0,
                "consistent_wallets", 0,
                "inconsistent_wallets", 0,
                "consistency_rate", "0%",
                "error", e.getMessage()
            );
        }
    }

    /**
     * Manual trigger for consistency check (can be called via API for testing)
     */
    public void manualCheckAndFix() {
        log.info("Manual wallet balance consistency check triggered");
        checkAndFixWalletBalance();
    }
}

