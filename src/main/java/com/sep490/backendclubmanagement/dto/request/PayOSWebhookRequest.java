package com.sep490.backendclubmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayOSWebhookRequest {
    private String code; // Response code ("00" = success)
    private String desc; // Description
    private Data data; // Payment data
    
    @lombok.Data
    public static class Data {
        private String accountNumber; // Số tài khoản nhận tiền
        private BigDecimal amount; // Số tiền
        private String description; // Mô tả
        private String reference; // Mã tham chiếu (VD: TF230204212323)
        private Long orderCode; // Mã đơn hàng từ PayOS, dùng để resolve được club,fee,member
        private String transactionDateTime; // Thời gian giao dịch (ISO format)
        private String currency; // Đơn vị tiền tệ
        private String paymentLinkId; // ID của payment link
        private String counterAccountBankId; // ID ngân hàng người chuyển
        private String counterAccountBankName; // Tên ngân hàng người chuyển
        private String counterAccountName; // Tên người chuyển
        private String counterAccountNumber; // Số tài khoản người chuyển
        private String virtualAccountName; // Tên tài khoản ảo
        private String virtualAccountNumber; // Số tài khoản ảo
        private String paymentStatus; // Trạng thái thanh toán
        private String paymentMethod; // Phương thức thanh toán
        private String transactionCode; // Mã giao dịch
    }
}

