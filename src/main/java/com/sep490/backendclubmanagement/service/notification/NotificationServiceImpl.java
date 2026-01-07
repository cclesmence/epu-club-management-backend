package com.sep490.backendclubmanagement.service.notification;

import com.sep490.backendclubmanagement.dto.response.NotificationResponse;
import com.sep490.backendclubmanagement.entity.Notification;
import com.sep490.backendclubmanagement.entity.NotificationPriority;
import com.sep490.backendclubmanagement.entity.NotificationType;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.NotificationRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.semester.SemesterService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final SemesterService semesterService;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final WebSocketService webSocketService;

    // ================== SEND ==================

    @Override
    @Transactional
    public void sendToUser(Long recipientId,
                           Long actorId,
                           String title,
                           String message,
                           NotificationType type,
                           NotificationPriority priority,
                           String actionUrl,
                           Long relatedClubId,
                           Long relatedNewsId,
                           Long relatedTeamId,
                           Long relatedRequestId,
                           Long relatedEventId) throws AppException {

        User recipient = userRepo.findById(recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User actor = null;
        if (actorId != null) {
            actor = userRepo.findById(actorId)
                    .orElse(null);
        }

        Notification noti = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .title(title)
                .message(message)
                .notificationType(type)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .isRead(false)
                .actionUrl(actionUrl)
                // các field liên quan entity Notification của bạn
                .relatedClubId(relatedClubId)
                .relatedPostId(null)
                .relatedEventId(relatedEventId)
                .relatedNewsId(relatedNewsId)
                .relatedFeeId(null)
                .relatedRecruitmentId(null)
                .relatedReportId(relatedRequestId)
                .relatedTeamId(relatedTeamId)
                // metadata để null
                .metadata(null)
                .build();

        notificationRepo.save(noti);

        // ✅ lỗi "Long cannot be converted to String" nằm ở đây
        // vì hàm sendToUser của WebSocketService đang nhận String payload
        webSocketService.sendToUser(
                recipient.getEmail(),           // ✅ String email
                "NOTIFICATION",
                "NEW",
                noti.getId().toString()         // payload là String id thông báo
        );    }

    @Override
    public void sendToUsers(List<Long> recipientIds, Long actorId, String title, String message,
                            NotificationType type, NotificationPriority priority, String actionUrl,
                            Long relatedClubId, Long relatedNewsId, Long relatedTeamId, Long relatedRequestId) {
        if (recipientIds == null || recipientIds.isEmpty()) return;
        for (Long id : recipientIds) {
            try {
                sendToUser(id, actorId, title, message, type, priority, actionUrl,
                        relatedClubId, relatedNewsId, relatedTeamId, relatedRequestId, null);
            } catch (AppException e) {
                // Không gửi được cho 1 user thì bỏ qua, tránh vỡ cả vòng
                // Có thể log ở đây nếu bạn muốn
            }
        }
    }

    @Override
    public List<Long> getClubManagers(Long clubId) throws AppException {
        Long semesterId = semesterService.getCurrentSemesterId();
        return clubMemberShipRepository.findManagerUserIdsByClubAndSemester(clubId, semesterId);
    }

    // ================== MAP DTO ==================

    private NotificationResponse toDto(Notification n) {
        User actor = n.getActor();
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType() != null ? n.getNotificationType().name() : null)
                .priority(n.getPriority() != null ? n.getPriority().name() : null)
                .read(Boolean.TRUE.equals(n.getIsRead()))
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .actionUrl(n.getActionUrl())
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actor != null ? actor.getFullName() : null)
                .actorAvatarUrl(actor != null ? actor.getAvatarUrl() : null)
                .build();
    }

    // ================== LIST / LATEST / COUNT ==================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId,
                                                         boolean unreadOnly,
                                                         int page,
                                                         int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Notification> p;
        if (unreadOnly) {
            p = notificationRepo.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            p = notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        }
        return p.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getLatestNotifications(Long userId,
                                                             boolean unreadOnly,
                                                             int limit) {
        int l = Math.max(limit, 1);
        List<Notification> list = unreadOnly
                ? notificationRepo.findTop10ByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepo.findTop10ByRecipientIdOrderByCreatedAtDesc(userId);
        if (list.size() > l) {
            list = list.subList(0, l);
        }
        return list.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepo.countByRecipientIdAndIsReadFalse(userId);
    }

    // ================== MARK READ ==================

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) throws AppException {
        Notification noti = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!noti.getRecipient().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (!Boolean.TRUE.equals(noti.getIsRead())) {
            noti.markAsRead();
            notificationRepo.save(noti);

            // 🔥 ADD: báo cho FE để Bell refresh
            webSocketService.sendToUser(
                    noti.getRecipient().getEmail(),
                    "NOTIFICATION",
                    "READ-UPDATE",
                    noti.getId().toString()   // payload FE không dùng cũng được
            );
        }
    }


    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        int updated = notificationRepo.markAllAsReadByUserId(userId);

        userRepo.findById(userId).ifPresent(u -> {
            webSocketService.sendToUser(
                    u.getEmail(),
                    "NOTIFICATION",
                    "READ-ALL",
                    updated + ""
            );
        });
    }


}
