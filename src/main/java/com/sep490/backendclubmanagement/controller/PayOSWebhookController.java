package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.PayOSWebhookRequest;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.fee.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pay-os")
@RequiredArgsConstructor
public class PayOSWebhookController {

    private final FeeService feeService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestBody PayOSWebhookRequest webhookRequest
    ) {
        try {
            // Log thông tin cơ bản từ webhook
            String orderCode = webhookRequest.getData() != null ? String.valueOf(webhookRequest.getData().getOrderCode()) : "N/A";
            String code = webhookRequest.getCode();
            log.info("[PayOS] Webhook received | orderCode={}, code={}", orderCode, code);


            if (webhookRequest.getData() == null || webhookRequest.getData().getOrderCode() == null) {
                log.warn("[PayOS] Webhook missing orderCode field");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "Missing orderCode in webhook data", null));
            }

            if (webhookRequest.getData().getOrderCode() == 123) {
                log.info("[PayOS] Test webhook received successfully");
                return ResponseEntity.ok(ApiResponse.success("Webhook test success"));
            }

            feeService.handlePaymentWebhook(webhookRequest);

            log.info("[PayOS] Webhook processed successfully for orderCode={}", orderCode);
            return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));

        } catch (AppException ex) {
            log.error("[PayOS] Application error while processing webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), null));

        } catch (Exception ex) {
            log.error("[PayOS] Unexpected error processing webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unexpected error processing webhook: " + ex.getMessage(),
                    null
            ));
        }
    }
}
