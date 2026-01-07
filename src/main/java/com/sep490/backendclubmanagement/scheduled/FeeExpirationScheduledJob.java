package com.sep490.backendclubmanagement.scheduled;

import com.sep490.backendclubmanagement.entity.fee.Fee;
import com.sep490.backendclubmanagement.repository.FeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to mark fees as locked (hasEverExpired = true) when:
 * 1. Their due date passes (expired fees)
 * 2. Someone has successfully paid for them
 *
 * Once a fee is marked as locked (hasEverExpired = true), the amount can never be edited again
 * This prevents scenarios where:
 * - A fee expires and admin tries to change the amount
 * - Someone pays a fee and admin tries to change the amount after
 * - Admin updates the due date to make an expired fee "active" and changes amount
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeExpirationScheduledJob {

    /**
     * Run once when application starts to check and lock fees
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("Application ready - running initial fee lock check");
        performFeeLockCheck();
    }

    private final FeeRepository feeRepository;

    /**
     * Check and lock fees every day at 00:05 AM
     * Locks fees that are either expired or have successful payments
     * Cron expression: "0 5 0 * * *" means:
     * - second: 0
     * - minute: 5
     * - hour: 0 (midnight)
     * - day of month: * (every day)
     * - month: * (every month)
     * - day of week: * (every day of week)
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void lockFees() {
        performFeeLockCheck();
    }

    /**
     * Core logic to check and lock fees that should be locked
     * Locks fees in two scenarios:
     * 1. Fees that have expired (due date has passed)
     * 2. Fees that have successful payment transactions
     */
    private void performFeeLockCheck() {
        log.info("Starting scheduled job: Lock Fees");

        try {
            LocalDate today = LocalDate.now();
            log.info("Checking for fees to lock on date: {}", today);

            // 1️⃣ Find all fees that have expired but not yet marked as hasEverExpired
            // Only check published fees (isDraft = false)
            List<Fee> expiredFees = feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(today);
            log.info("Found {} expired fees to lock", expiredFees.size());

            // 2️⃣ Find all fees that have successful payments but not yet marked as hasEverExpired
            List<Fee> feesWithPayments = feeRepository.findFeesWithSuccessfulPaymentsButNotLocked();
            log.info("Found {} fees with successful payments to lock", feesWithPayments.size());

            // 3️⃣ Combine both lists and remove duplicates
            java.util.Set<Fee> feesToLock = new java.util.HashSet<>();
            feesToLock.addAll(expiredFees);
            feesToLock.addAll(feesWithPayments);

            if (feesToLock.isEmpty()) {
                log.info("No fees to lock");
                return;
            }

            log.info("Total {} unique fees to lock", feesToLock.size());

            // 4️⃣ Mark all fees as locked
            int lockedCount = 0;
            for (Fee fee : feesToLock) {
                String reason = "";
                if (expiredFees.contains(fee)) {
                    reason += "EXPIRED";
                }
                if (feesWithPayments.contains(fee)) {
                    reason += (reason.isEmpty() ? "" : " & ") + "HAS_PAYMENT";
                }

                log.debug("Locking fee: ID={}, Title='{}', Reason={}, DueDate={}",
                        fee.getId(), fee.getTitle(), reason, fee.getDueDate());

                fee.setHasEverExpired(true);
                lockedCount++;
            }

            // 5️⃣ Save all changes
            feeRepository.saveAll(feesToLock);

            log.info("Successfully locked {} fees (amount can no longer be edited)", lockedCount);

        } catch (Exception e) {
            log.error("Error occurred while locking fees: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }

        log.info("Completed scheduled job: Lock Fees");
    }
}

