package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.NotificationResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.NotificationRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.notification.NotificationServiceImpl;
import com.sep490.backendclubmanagement.service.semester.SemesterService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {


    @Mock private NotificationRepository notificationRepo;
    @Mock private UserRepository userRepo;
    @Mock private SemesterService semesterService;
    @Mock private ClubMemberShipRepository clubMemberShipRepository;
    @Mock private WebSocketService webSocketService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private User actor;

    @BeforeEach
    void setup() {
        recipient = new User();
        recipient.setId(1L);
        recipient.setEmail("recipient@example.com");
        recipient.setFullName("Recipient");

        actor = new User();
        actor.setId(2L);
        actor.setEmail("actor@example.com");
        actor.setFullName("Actor");
    }

    // =====================================================================
    // SEND TO USER
    // =====================================================================

    @Test
    void sendToUser_happyPath_savesNotificationAndSendWebSocket() throws AppException {
        when(userRepo.findById(1L)).thenReturn(Optional.of(recipient));
        when(userRepo.findById(2L)).thenReturn(Optional.of(actor));

        when(notificationRepo.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(999L);
            return n;
        });

        notificationService.sendToUser(
                1L, 2L,
                "Title", "Message",
                NotificationType.NEWS_APPROVED,
                NotificationPriority.HIGH,
                "/url",
                10L, 20L, 30L, 40L, 50L
        );

        verify(notificationRepo).save(any(Notification.class));
        verify(webSocketService).sendToUser(eq("recipient@example.com"), eq("NOTIFICATION"), eq("NEW"), anyString());
    }

    @Test
    void sendToUser_recipientNotFound_throws() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                notificationService.sendToUser(
                        1L, 2L,
                        "T", "M",
                        NotificationType.GENERAL,
                        NotificationPriority.NORMAL,
                        null,
                        1L, null, null, null, null
                ));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void sendToUser_actorNotFound_stillWorksButActorNull() throws AppException {
        when(userRepo.findById(1L)).thenReturn(Optional.of(recipient));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        when(notificationRepo.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.sendToUser(
                1L, 2L,
                "T", "M",
                NotificationType.GENERAL,
                NotificationPriority.NORMAL,
                null,
                1L, null, null, null, null
        );

        verify(webSocketService).sendToUser(anyString(), eq("NOTIFICATION"), eq("NEW"), anyString());
    }

    // =====================================================================
    // SEND TO USERS
    // =====================================================================

    @Test
    void sendToUsers_happyPath() {
        List<Long> ids = List.of(1L, 2L);

        User u1 = new User(); u1.setId(1L); u1.setEmail("u1@x.com");
        User u2 = new User(); u2.setId(2L); u2.setEmail("u2@x.com");

        when(userRepo.findById(1L)).thenReturn(Optional.of(u1));
        when(userRepo.findById(2L)).thenReturn(Optional.of(u2));
        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        when(notificationRepo.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(111L);
            return n;
        });

        notificationService.sendToUsers(
                ids, 999L,
                "Title", "Msg",
                NotificationType.GENERAL,
                NotificationPriority.LOW,
                "/a",
                1L, null, null, null
        );

        verify(webSocketService, times(2)).sendToUser(anyString(), eq("NOTIFICATION"), eq("NEW"), anyString());
    }

    @Test
    void sendToUsers_ignoreInvalidRecipients() {
        when(userRepo.findById(anyLong())).thenReturn(Optional.empty());

        notificationService.sendToUsers(
                List.of(1L, 2L),
                99L,
                "T", "M",
                NotificationType.GENERAL,
                NotificationPriority.NORMAL,
                null,
                1L, null, null, null
        );

        verify(notificationRepo, never()).save(any());
        verify(webSocketService, never()).sendToUser(anyString(), anyString(), anyString(), anyString());
    }

    // =====================================================================
    // GET MY NOTIFICATIONS
    // =====================================================================

    @Test
    void getMyNotifications_unreadOnlyFalse_returnsAll() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 5);

        Notification noti = mock(Notification.class);
        when(noti.getId()).thenReturn(1L);
        when(noti.getTitle()).thenReturn("T");
        when(noti.getMessage()).thenReturn("M");
        when(noti.getIsRead()).thenReturn(true);

        Page<Notification> page = new PageImpl<>(List.of(noti), pageable, 1);
        when(notificationRepo.findByRecipientIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(page);

        Page<NotificationResponse> resp =
                notificationService.getMyNotifications(userId, false, 0, 5);

        assertEquals(1, resp.getTotalElements());
        assertTrue(resp.getContent().get(0).getRead());
    }

    @Test
    void getMyNotifications_unreadOnlyTrue_returnsUnread() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 5);

        Notification noti = mock(Notification.class);
        when(noti.getId()).thenReturn(10L);
        when(noti.getTitle()).thenReturn("A");
        when(noti.getMessage()).thenReturn("B");
        when(noti.getNotificationType()).thenReturn(NotificationType.NEWS_APPROVED);
        when(noti.getPriority()).thenReturn(NotificationPriority.HIGH);
        when(noti.getIsRead()).thenReturn(false);
        when(noti.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(noti.getReadAt()).thenReturn(null);
        when(noti.getActionUrl()).thenReturn("/url");

        User a = new User();
        a.setId(2L);
        a.setFullName("Actor");
        a.setAvatarUrl("ava.png");
        when(noti.getActor()).thenReturn(a);

        Page<Notification> page = new PageImpl<>(List.of(noti), pageable, 1);
        when(notificationRepo.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(page);

        Page<NotificationResponse> resp =
                notificationService.getMyNotifications(userId, true, 0, 10);

        NotificationResponse dto = resp.getContent().get(0);
        assertEquals("A", dto.getTitle());
        assertEquals("B", dto.getMessage());
        assertEquals(NotificationPriority.HIGH.name(), dto.getPriority());
        assertEquals("Actor", dto.getActorName());
    }

    // =====================================================================
    // GET LATEST
    // =====================================================================

    @Test
    void getLatestNotifications_truncatesLimitCorrectly() {
        Long userId = 1L;

        Notification n1 = mock(Notification.class);
        when(n1.getId()).thenReturn(1L);

        Notification n2 = mock(Notification.class);
        when(n2.getId()).thenReturn(2L);

        Notification n3 = mock(Notification.class);
        when(n3.getId()).thenReturn(3L);

        when(notificationRepo.findTop10ByRecipientIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(n1, n2, n3));

        List<NotificationResponse> resp =
                notificationService.getLatestNotifications(userId, false, 2);

        assertEquals(2, resp.size());
        assertEquals(1L, resp.get(0).getId());
        assertEquals(2L, resp.get(1).getId());
    }

    @Test
    void getLatestNotifications_limitMoreThanSize_returnsAll() {
        Long userId = 1L;

        Notification n1 = mock(Notification.class);
        when(n1.getId()).thenReturn(1L);

        // Chỉ mock đúng method (unreadOnly = false)
        when(notificationRepo.findTop10ByRecipientIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(n1));

        List<NotificationResponse> resp =
                notificationService.getLatestNotifications(userId, false, 10);

        assertEquals(1, resp.size());
    }

    // =====================================================================
    // COUNT UNREAD
    // =====================================================================

    @Test
    void countUnread_returnsValue() {
        when(notificationRepo.countByRecipientIdAndIsReadFalse(1L))
                .thenReturn(5L);

        assertEquals(5L, notificationService.countUnread(1L));
    }

    // =====================================================================
    // MARK AS READ
    // =====================================================================

    @Test
    void markAsRead_happyPath() throws AppException {
        Long userId = 1L;
        Long notiId = 10L;

        Notification noti = mock(Notification.class);
        when(noti.getRecipient()).thenReturn(recipient);
        when(noti.getIsRead()).thenReturn(false);
        when(noti.getId()).thenReturn(notiId);

        when(notificationRepo.findById(notiId))
                .thenReturn(Optional.of(noti));

        notificationService.markAsRead(userId, notiId);

        verify(noti).markAsRead();
        verify(notificationRepo).save(noti);
        verify(webSocketService).sendToUser(eq("recipient@example.com"),
                eq("NOTIFICATION"), eq("READ-UPDATE"), eq("10"));
    }

    @Test
    void markAsRead_forbiddenUser_throws() {
        Notification noti = mock(Notification.class);

        User other = new User();
        other.setId(99L);

        when(noti.getRecipient()).thenReturn(other);
        when(notificationRepo.findById(10L)).thenReturn(Optional.of(noti));

        AppException ex = assertThrows(AppException.class,
                () -> notificationService.markAsRead(1L, 10L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void markAsRead_alreadyRead_noAction() throws AppException {
        Long userId = 1L;
        Long notiId = 10L;

        Notification noti = mock(Notification.class);
        when(noti.getRecipient()).thenReturn(recipient);
        when(noti.getIsRead()).thenReturn(true);

        when(notificationRepo.findById(notiId))
                .thenReturn(Optional.of(noti));

        notificationService.markAsRead(userId, notiId);

        verify(noti, never()).markAsRead();
        verify(notificationRepo, never()).save(any());
    }

    // =====================================================================
    // MARK ALL
    // =====================================================================

    @Test
    void markAllAsRead_happyPath() {
        Long userId = 1L;

        // Giả lập repo cập nhật 2 bản ghi
        when(notificationRepo.markAllAsReadByUserId(userId)).thenReturn(2);

        User u = new User();
        u.setId(userId);
        u.setEmail("recipient@example.com");
        when(userRepo.findById(userId)).thenReturn(Optional.of(u));

        notificationService.markAllAsRead(userId);

        verify(notificationRepo).markAllAsReadByUserId(userId);

        // Socket báo READ-ALL
        verify(webSocketService).sendToUser(
                eq("recipient@example.com"),
                eq("NOTIFICATION"),
                eq("READ-ALL"),
                eq("2")
        );
    }


    @Test
    void markAllAsRead_userNotFound_noSocket() {
        Long userId = 1L;

        when(notificationRepo.markAllAsReadByUserId(userId)).thenReturn(5);
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        notificationService.markAllAsRead(userId);

        verify(notificationRepo).markAllAsReadByUserId(userId);
        verify(webSocketService, never()).sendToUser(anyString(), anyString(), anyString(), anyString());
    }

}
