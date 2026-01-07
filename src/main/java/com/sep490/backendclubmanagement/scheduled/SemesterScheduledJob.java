package com.sep490.backendclubmanagement.scheduled;

import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Scheduled job to update current semester daily
 * Runs every day at 00:01 AM to check and update the current semester
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SemesterScheduledJob {

    /**
     * Run once when application starts to initialize current semester
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("Application ready - running initial semester update");
        performSemesterUpdate();
    }

    private final SemesterRepository semesterRepository;

    /**
     * Check and update current semester every day at 00:01 AM
     * Cron expression: "0 1 0 * * *" means:
     * - second: 0
     * - minute: 1
     * - hour: 0 (midnight)
     * - day of month: * (every day)
     * - month: * (every month)
     * - day of week: * (every day of week)
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void updateCurrentSemester() {
        performSemesterUpdate();
    }

    /**
     * Core logic to update current semester
     */
    private void performSemesterUpdate() {
        log.info("Starting scheduled job: Update Current Semester");

        try {
            LocalDate today = LocalDate.now();
            log.info("Checking for current semester on date: {}", today);

            // Find semester that should be current (based on today's date)
            Optional<Semester> semesterOpt = semesterRepository.findSemesterByDate(today);

            if (semesterOpt.isPresent()) {
                Semester targetSemester = semesterOpt.get();

                // Check if this semester is already marked as current
                if (Boolean.TRUE.equals(targetSemester.getIsCurrent())) {
                    log.info("Semester '{}' (ID: {}) is already marked as current. No update needed.",
                            targetSemester.getSemesterName(), targetSemester.getId());
                    return;
                }

                // Set all semesters to not current
                log.info("Setting all semesters to isCurrent = false");
                semesterRepository.setAllSemestersNotCurrent();

                // Set the target semester as current
                log.info("Setting semester '{}' (ID: {}) as current",
                        targetSemester.getSemesterName(), targetSemester.getId());
                semesterRepository.setCurrentSemester(targetSemester.getId());

                log.info("Successfully updated current semester to: '{}' (ID: {}), Period: {} to {}",
                        targetSemester.getSemesterName(),
                        targetSemester.getId(),
                        targetSemester.getStartDate(),
                        targetSemester.getEndDate());

            } else {
                log.warn("No semester found for date: {}. No semester is marked as current.", today);

                // Set all semesters to not current since we're not in any semester period
                semesterRepository.setAllSemestersNotCurrent();
                log.info("All semesters set to isCurrent = false (not in any semester period)");
            }

        } catch (Exception e) {
            log.error("Error occurred while updating current semester: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }

        log.info("Completed scheduled job: Update Current Semester");
    }
}

