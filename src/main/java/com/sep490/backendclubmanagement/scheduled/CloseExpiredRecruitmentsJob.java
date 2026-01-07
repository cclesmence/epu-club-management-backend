package com.sep490.backendclubmanagement.scheduled;

import com.sep490.backendclubmanagement.service.recruitment.RecruitmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Scheduled job to close recruitments whose endDate has passed.
 * Runs hourly and once at application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloseExpiredRecruitmentsJob {

    private final RecruitmentService recruitmentService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started - running initial expired recruitments sweep");
        runCloseExpiredRecruitments();
    }

    /**
     * Run hourly: at minute 0 of every hour.
     * Cron: second minute hour day month weekday
     * Here we use "0 0 * * * *" -> every hour at HH:00:00
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledCloseExpiredRecruitments() {
        runCloseExpiredRecruitments();
    }

    private void runCloseExpiredRecruitments() {
        try {
            // Use UTC-based now to avoid timezone mismatches with DB (assume DB stores UTC)
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            log.info("Closing expired recruitments - checking endDate < {} (UTC)", nowUtc);
            int updated = recruitmentService.closeExpiredRecruitments(nowUtc);
            log.info("Closed {} expired recruitment(s)", updated);
        } catch (Exception e) {
            log.error("Error while closing expired recruitments: {}", e.getMessage(), e);
        }
    }
}

