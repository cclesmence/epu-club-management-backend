package com.sep490.backendclubmanagement.service.notification;

import com.sep490.backendclubmanagement.dto.response.NotificationResponse;
import com.sep490.backendclubmanagement.entity.NotificationPriority;
import com.sep490.backendclubmanagement.entity.NotificationType;
import com.sep490.backendclubmanagement.exception.AppException;

import java.util.List;

public interface NotificationService {

    // ==== các hàm SEND (bạn đã dùng trong News*) ====
    void sendToUser(Long recipientId,
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
                    Long relatedEventId) throws AppException;

    void sendToUsers(List<Long> recipientIds,
                     Long actorId,
                     String title,
                     String message,
                     NotificationType type,
                     NotificationPriority priority,
                     String actionUrl,
                     Long relatedClubId,
                     Long relatedNewsId,
                     Long relatedTeamId,
                     Long relatedRequestId);

    List<Long> getClubManagers(Long clubId) throws AppException; // dùng như mình đã code trong News*

    // ==== các hàm ĐỌC / ĐÁNH DẤU ĐÃ ĐỌC ====

    /** Trang "Thông báo": phân trang, lọc tất cả hoặc chỉ chưa đọc */
    org.springframework.data.domain.Page<NotificationResponse> getMyNotifications(
            Long userId, boolean unreadOnly, int page, int size
    );

    /** Popup chuông: lấy vài thông báo mới nhất */
    List<NotificationResponse> getLatestNotifications(Long userId, boolean unreadOnly, int limit);

    /** Badge số thông báo chưa đọc trên icon chuông */
    long countUnread(Long userId);

    /** Đánh dấu 1 thông báo là đã đọc */
    void markAsRead(Long userId, Long notificationId) throws AppException;

    /** Đánh dấu TẤT CẢ thông báo của user là đã đọc */
    void markAllAsRead(Long userId);
}
