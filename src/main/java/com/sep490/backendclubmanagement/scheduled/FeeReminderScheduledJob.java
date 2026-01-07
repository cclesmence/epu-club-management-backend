package com.sep490.backendclubmanagement.scheduled;

import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import com.sep490.backendclubmanagement.entity.fee.FeeType;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.email.EmailService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled job to send reminders for fees that are about to expire
 * Sends notifications and emails to users who haven't paid mandatory fees
 *
 * Reminder schedule:
 * - 7 days before due date
 * - 3 days before due date
 * - 1 day before due date
 * - On due date
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeReminderScheduledJob {

    private final FeeRepository feeRepository;
    private final IncomeTransactionRepository incomeTransactionRepository;
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Check and send reminders every day at 09:00 AM
     * Cron expression: "0 0 9 * * *" means:
     * - second: 0
     * - minute: 0
     * - hour: 9 (9 AM)
     * - day of month: * (every day)
     * - month: * (every month)
     * - day of week: * (every day of week)
     */
//    @EventListener(ApplicationReadyEvent.class)
//    @Transactional
//    public void onApplicationReady() {
//        log.info("Application ready - running initial fee reminder check");
//        sendFeeReminders();
//    }



    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendFeeReminders() {
        log.info("Starting scheduled job: Send Fee Reminders");

        try {
            LocalDate today = LocalDate.now();

            // Days to check: 7, 3, 1 days before and on due date
            List<Integer> reminderDays = Arrays.asList(7, 3, 1, 0);

            for (Integer daysBeforeDue : reminderDays) {
                LocalDate targetDate = today.plusDays(daysBeforeDue);
                sendRemindersForDate(targetDate, daysBeforeDue);
            }

            log.info("Completed scheduled job: Send Fee Reminders");

        } catch (Exception e) {
            log.error("Error in FeeReminderScheduledJob: {}", e.getMessage(), e);
        }
    }

    /**
     * Send reminders for fees due on a specific date
     */
    private void sendRemindersForDate(LocalDate dueDate, int daysBeforeDue) {
        log.info("Checking fees due on: {} ({} days before)", dueDate, daysBeforeDue);

        try {
            LocalDate today = LocalDate.now();
            // Find all published mandatory fees with this due date that haven't expired yet
            List<Fee> fees = feeRepository.findAll().stream()
                    .filter(fee -> !fee.getIsDraft())
                    .filter(Fee::getIsMandatory)
                    .filter(fee -> fee.getDueDate() != null && fee.getDueDate().equals(dueDate))
                    .filter(fee -> !fee.getDueDate().isBefore(today))
                    .toList();

            log.info("Found {} mandatory fees due on {}", fees.size(), dueDate);

            for (Fee fee : fees) {
                try {
                    sendRemindersForFee(fee, daysBeforeDue);
                } catch (Exception e) {
                    log.error("Error sending reminders for fee {}: {}", fee.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error checking fees for date {}: {}", dueDate, e.getMessage(), e);
        }
    }

    /**
     * Send reminders to all unpaid members for a specific fee
     */
    private void sendRemindersForFee(Fee fee, int daysBeforeDue) {
        log.info("Processing reminders for fee: {} ({})", fee.getId(), fee.getTitle());

        // Get all active members in the club
        List<Long> activeMemberIds = roleMemberShipRepository
                .findActiveMemberUserIdsByClubId(fee.getClub().getId());

        if (activeMemberIds.isEmpty()) {
            log.info("No active members found for club {}", fee.getClub().getId());
            return;
        }

        // Filter out members who have already paid
        List<Long> unpaidMemberIds = activeMemberIds.stream()
                .filter(userId -> !hasPaidFee(fee.getId(), userId))
                .collect(Collectors.toList());

        log.info("Found {} unpaid members out of {} active members",
                unpaidMemberIds.size(), activeMemberIds.size());

        if (unpaidMemberIds.isEmpty()) {
            log.info("All members have paid for fee {}", fee.getId());
            return;
        }

        // Send notifications
        sendNotifications(fee, unpaidMemberIds, daysBeforeDue);

        // Send emails
        sendEmails(fee, unpaidMemberIds, daysBeforeDue);

        log.info("Sent reminders to {} members for fee {}", unpaidMemberIds.size(), fee.getId());
    }

    /**
     * Check if a user has paid for a fee
     */
    private boolean hasPaidFee(Long feeId, Long userId) {
        return incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(
                userId, feeId, TransactionStatus.SUCCESS);
    }

    /**
     * Send in-app notifications to unpaid members
     */
    private void sendNotifications(Fee fee, List<Long> unpaidMemberIds, int daysBeforeDue) {
        try {
            String title = buildNotificationTitle(daysBeforeDue);
            String message = buildNotificationMessage(fee, daysBeforeDue);
            String actionUrl = "/clubs/" + fee.getClub().getId() + "/fees/" + fee.getId();

            notificationService.sendToUsers(
                    unpaidMemberIds,
                    null, // actor (system notification)
                    title,
                    message,
                    NotificationType.FEE_REMINDER,
                    NotificationPriority.HIGH,
                    actionUrl,
                    fee.getClub().getId(),
                    null, // relatedNewsId
                    null, // relatedTeamId
                    null  // relatedRequestId
            );

            log.info("Sent notifications to {} users for fee {}", unpaidMemberIds.size(), fee.getId());

        } catch (Exception e) {
            log.error("Failed to send notifications for fee {}: {}", fee.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send emails to unpaid members using Thymeleaf template
     */
    private void sendEmails(Fee fee, List<Long> unpaidMemberIds, int daysBeforeDue) {
        try {
            List<User> unpaidUsers = userRepository.findAllById(unpaidMemberIds);

            for (User user : unpaidUsers) {
                try {
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        String subject = buildEmailSubject(fee, daysBeforeDue);
                        Map<String, Object> variables = buildTemplateVariables(fee, user, daysBeforeDue);

                        emailService.sendTemplatedEmail(
                                user.getEmail(),
                                subject,
                                "email/fee-reminder",  // Template name
                                variables
                        );

                        log.debug("Sent templated email to {} for fee {}", user.getEmail(), fee.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to send email to user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("Sent emails to {} users for fee {}", unpaidUsers.size(), fee.getId());

        } catch (Exception e) {
            log.error("Failed to send emails for fee {}: {}", fee.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build notification title based on days before due
     */
    private String buildNotificationTitle(int daysBeforeDue) {
        if (daysBeforeDue == 0) {
            return "⚠️ HÔM NAY là hạn đóng phí!";
        } else if (daysBeforeDue == 1) {
            return "⚠️ Nhắc nhở: Còn 1 ngày để đóng phí";
        } else {
            return "📢 Nhắc nhở: Còn " + daysBeforeDue + " ngày để đóng phí";
        }
    }

    /**
     * Build notification message
     */
    private String buildNotificationMessage(Fee fee, int daysBeforeDue) {
        StringBuilder message = new StringBuilder();
        message.append("Khoản phí: ").append(fee.getTitle()).append("\n");
        message.append("Số tiền: ").append(String.format("%,d", fee.getAmount().longValue())).append(" VND\n");

        if (daysBeforeDue == 0) {
            message.append("Hạn cuối: HÔM NAY (").append(fee.getDueDate()).append(")\n");
        } else {
            message.append("Hạn cuối: ").append(fee.getDueDate()).append("\n");
        }

        message.append("\nVui lòng đóng phí trước hạn để tránh bị phạt!");

        return message.toString();
    }

    /**
     * Build email subject
     */
    private String buildEmailSubject(Fee fee, int daysBeforeDue) {
        if (daysBeforeDue == 0) {
            return "[" + fee.getClub().getClubName() + "] HÔM NAY là hạn đóng phí: " + fee.getTitle();
        } else if (daysBeforeDue == 1) {
            return "[" + fee.getClub().getClubName() + "] Còn 1 ngày để đóng phí: " + fee.getTitle();
        } else {
            return "[" + fee.getClub().getClubName() + "] Nhắc nhở đóng phí: " + fee.getTitle();
        }
    }

    /**
     * Build template variables for Thymeleaf
     */
    private Map<String, Object> buildTemplateVariables(Fee fee, User user, int daysBeforeDue) {
        Map<String, Object> variables = new java.util.HashMap<>();
        
        // User info
        variables.put("userName", user.getFullName());
        
        // Club info
        variables.put("clubName", fee.getClub().getClubName());
        
        // Fee info
        variables.put("feeTitle", fee.getTitle());
        variables.put("feeDescription", fee.getDescription());
        variables.put("feeAmount", fee.getAmount());
        variables.put("feeDueDate", fee.getDueDate());
        variables.put("feeType", fee.getFeeType() == FeeType.MEMBERSHIP ? "Phí thành viên" : "Phí hoạt động");
        
        // Semester info (if available)
        if (fee.getSemester() != null) {
            variables.put("semesterName", fee.getSemester().getSemesterName());
        }
        
        // Days before due
        variables.put("daysBeforeDue", daysBeforeDue);
        
        // Alert styling based on urgency
        if (daysBeforeDue == 0) {
            variables.put("alertClass", "alert-urgent");
            variables.put("alertIcon", "🚨");
            variables.put("alertMessage", "HÔM NAY là hạn đóng phí bắt buộc!");
        } else if (daysBeforeDue == 1) {
            variables.put("alertClass", "alert-high");
            variables.put("alertIcon", "⚠️");
            variables.put("alertMessage", "Chỉ còn 1 ngày để đóng phí bắt buộc!");
        } else if (daysBeforeDue == 3) {
            variables.put("alertClass", "alert-high");
            variables.put("alertIcon", "⚠️");
            variables.put("alertMessage", "Còn " + daysBeforeDue + " ngày để đóng phí");
        } else {
            variables.put("alertClass", "alert-normal");
            variables.put("alertIcon", "📢");
            variables.put("alertMessage", "Nhắc nhở: Còn " + daysBeforeDue + " ngày để đóng phí");
        }
        
        // Action URL
        String actionUrl = getFrontendUrl() + "/myclub/" + fee.getClub().getId() + "/payments";
        variables.put("actionUrl", actionUrl);
        
        // Current year for footer
        variables.put("currentYear", java.time.Year.now().getValue());
        
        return variables;
    }

    /**
     * Get frontend URL (should be from config)
     */
    private String getFrontendUrl() {
        // TODO: Get from application.yml
        return "http://localhost:5173";
    }
}

