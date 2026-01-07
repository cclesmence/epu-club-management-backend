package com.sep490.backendclubmanagement.service.websocket;

import com.sep490.backendclubmanagement.dto.websocket.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    public <T> void sendToUser(String email, String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSendToUser(
                email,
                "/queue/messages",
                message
            );
            log.info("Sent message to user: {}, type: {}, action: {}", email, type, action);
        } catch (Exception e) {
            log.error("Failed to send message to user: {}", email, e);
        }
    }

    public void sendPaymentSuccessByEmail(String email, PaymentWebSocketPayload payload) {
        try {
            WebSocketMessage<PaymentWebSocketPayload> message = WebSocketMessage.of(
                    WebSocketMessageType.PAYMENT.name(),
                    WebSocketMessageAction.SUCCESS.name(),
                    payload
            );

            log.info("=== SENDING WEBSOCKET MESSAGE ===");
            log.info("To user email: {}", email);

            // Tìm user session bằng email
            SimpUser user = userRegistry.getUser(email);

            if (user == null) {
                log.error("❌ User not found in registry: {}", email);
                log.info("Available users: {}",
                        userRegistry.getUsers().stream()
                                .map(SimpUser::getName)
                                .collect(Collectors.toList())
                );
                return;
            }

            log.info("✅ Found user with {} sessions", user.getSessions().size());

            // Gửi đến tất cả sessions của user
            for (SimpSession session : user.getSessions()) {
                String sessionId = session.getId();
                log.info("Sending to session: {}", sessionId);

                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/messages",
                        message,
                        createHeaders(sessionId)
                );
            }

            log.info("=== MESSAGE SENT TO {} SESSIONS ===", user.getSessions().size());
        } catch (Exception e) {
            log.error("=== FAILED TO SEND MESSAGE ===", e);
        }
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

    public void sendPaymentSuccess(String email, PaymentWebSocketPayload payload) {
        sendToUser(email,
            WebSocketMessageType.PAYMENT.name(),
            WebSocketMessageAction.SUCCESS.name(),
            payload
        );

        log.info("=== SENDING WEBSOCKET MESSAGE ===");
        log.info("To user: {}", email);
        log.info("Destination: /queue/messages");
        log.info("Full destination: /user/{}/queue/messages", email);


    }
    public void sendPaymentFailed(String email, PaymentWebSocketPayload payload) {
        sendToUser(email,
            WebSocketMessageType.PAYMENT.name(),
            WebSocketMessageAction.FAILED.name(),
            payload
        );
    }
    public void sendNotificationToUser(String email, NotificationWebSocketPayload payload) {
        sendToUser(email,
            WebSocketMessageType.NOTIFICATION.name(),
            WebSocketMessageAction.INFO.name(),
            payload
        );
    }
    public <T> void broadcastToClub(Long clubId, String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSend(
                "/topic/club/" + clubId,
                message
            );
            log.info("Broadcast to club: {}, type: {}, action: {}", clubId, type, action);
        } catch (Exception e) {
            log.error("Failed to broadcast to club: {}", clubId, e);
        }
    }
    public <T> void broadcastToClubRole(Long clubId, String roleName, String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSend(
                "/topic/club/" + clubId + "/role/" + roleName,
                message
            );
            log.info("Broadcast to club: {}, role: {}, type: {}", clubId, roleName, type);
        } catch (Exception e) {
            log.error("Failed to broadcast to club role: {}", clubId, e);
        }
    }
    public <T> void broadcastToSystemRole(String systemRole, String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSend(
                "/topic/system/role/" + systemRole,
                message
            );
            log.info("Broadcast to system role: {}, type: {}", systemRole, type);
        } catch (Exception e) {
            log.error("Failed to broadcast to system role: {}", systemRole, e);
        }
    }
    public <T> void broadcastToTeam(Long teamId, String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSend(
                "/topic/team/" + teamId,
                message
            );
            log.info("Broadcast to team: {}, type: {}", teamId, type);
        } catch (Exception e) {
            log.error("Failed to broadcast to team: {}", teamId, e);
        }
    }
    public <T> void broadcastSystemWide(String type, String action, T payload) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSend("/topic/system/all", message);
            log.info("System-wide broadcast: type: {}, action: {}", type, action);
        } catch (Exception e) {
            log.error("Failed to broadcast system-wide message", e);
        }
    }
    public <T> void sendToMultipleUsers(List<String> emails, String type, String action, T payload) {
        WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
        emails.forEach(email -> {
            try {
                messagingTemplate.convertAndSendToUser(
                    email,
                    "/queue/messages",
                    message
                );
            } catch (Exception e) {
                log.error("Failed to send message to user: {}", email, e);
            }
        });
        log.info("Sent message to {} users, type: {}", emails.size(), type);
    }
    public <T> void broadcastToUser(Long userId, String type, String action, T payload) {
        try {
            if (userId == null) {
                log.warn("broadcastToUser skipped: null userId");
                return;
            }
            WebSocketMessage<T> message = WebSocketMessage.of(type, action, payload);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/" + type,
                    message
            );
            log.info("Broadcast to userId: {}, type: {}, action: {}", userId, type, action);
        } catch (Exception e) {
            log.error("Failed to broadcast to userId: {}", userId, e);
        }
    }

}

















