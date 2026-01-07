package com.sep490.backendclubmanagement.controller;



import com.sep490.backendclubmanagement.dto.websocket.PaymentWebSocketPayload;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/api/test")
@Validated
@RequiredArgsConstructor
public class TestController {

    private final WebSocketService webSocketService;

    @PostMapping("/send-payment-notification")
    public ResponseEntity<?> testPaymentNotification(
            @RequestParam String email,
            @RequestParam Long feeId
    ) {
        PaymentWebSocketPayload payload = PaymentWebSocketPayload.builder()
                .userId(1L)
                .feeId(feeId)
                .amount(new BigDecimal("100000"))
                .orderCode(123456L)
                .status("SUCCESS")
                .transactionCode("TEST123")
                .message("Test payment notification")
                .build();

        webSocketService.sendPaymentSuccess(email, payload);

        return ResponseEntity.ok("Notification sent to " + email);
    }
}
