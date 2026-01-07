package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho thông tin người đã đóng phí
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaidMemberResponse {
    private Long userId;
    private String studentCode;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;

    // Thông tin giao dịch
    private Long transactionId;
    private BigDecimal paidAmount;
    private LocalDateTime paidDate;
    private String transactionReference;
    private String transactionStatus;
    private String paymentMethod;

    // Thông tin bổ sung
    private String semesterName;
    private String roleName;
    private String teamName;
}

