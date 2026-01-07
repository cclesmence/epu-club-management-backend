package com.sep490.backendclubmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSIncomingTransferResponse {
    private String id; // Payment ID từ PayOS
    private BigDecimal amount; // Số tiền (VND)
    private String status; // e.g., "PAID", "PENDING", "CANCELLED"
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt; // Thời gian tạo
    private String description; // Mô tả chuyển khoản
    private String payerName; // Tên người chuyển
    private String payerEmail; // Email người chuyển (nếu có)
    private String bin;
}
