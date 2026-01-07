package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.NotificationResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    // Trang "Thông báo" (ảnh 1)
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> list(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(notificationService.getMyNotifications(me, unreadOnly, page, size));
    }

    // Popup chuông: lấy vài cái mới nhất (ảnh 2)
    @GetMapping("/latest")
    public ApiResponse<List<NotificationResponse>> latest(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "5") int limit
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(notificationService.getLatestNotifications(me, unreadOnly, limit));
    }

    // Badge số thông báo chưa đọc trên icon chuông
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(
            @AuthenticationPrincipal User principal
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(notificationService.countUnread(me));
    }

    // Đánh dấu 1 notification là đã đọc (click vào 1 dòng)
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal User principal,
            @PathVariable Long id
    ) throws AppException {
        Long me = userService.getIdByEmail(principal.getUsername());
        notificationService.markAsRead(me, id);
        return ApiResponse.success(null);
    }

    // Đánh dấu tất cả là đã đọc (nút "Đánh dấu tất cả đã đọc")
    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllRead(
            @AuthenticationPrincipal User principal
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        notificationService.markAllAsRead(me);
        return ApiResponse.success(null);
    }
}
