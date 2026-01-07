package com.sep490.backendclubmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payos_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayOSPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Response fields
    @Column(name = "code", length = 10)
    private String code; // Mã phản hồi từ PayOS ("00" = success)

    @Column(name = "success")
    private Boolean success; // Trạng thái thành công

    @Column(name = "signature", length = 500)
    private String signature; // Chữ ký để verify webhook

    // Payment data fields
    @Column(name = "transaction_code", unique = true, length = 100)
    private String transactionCode;

    @Column(name = "order_code", unique = true, length = 100)
    private String orderCode;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "account_number", length = 50)
    private String accountNumber; // Số tài khoản nhận tiền

    @Column(name = "reference", length = 100)
    private String reference; // Mã tham chiếu giao dịch (VD: TF230204212323)

    @Column(name = "transaction_date_time")
    private LocalDateTime transactionDateTime; // Thời gian giao dịch thực tế từ PayOS

    @Column(name = "currency", length = 10)
    private String currency; // Đơn vị tiền tệ (VND)

    @Column(name = "payment_link_id", length = 100)
    private String paymentLinkId; // ID của payment link

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    // Counter account information (người chuyển tiền)
    @Column(name = "counter_account_bank_id", length = 50)
    private String counterAccountBankId; // ID ngân hàng người chuyển

    @Column(name = "counter_account_bank_name", length = 255)
    private String counterAccountBankName; // Tên ngân hàng người chuyển

    @Column(name = "counter_account_name", length = 255)
    private String counterAccountName; // Tên người chuyển

    @Column(name = "counter_account_number", length = 50)
    private String counterAccountNumber; // Số tài khoản người chuyển

    // Virtual account information
    @Column(name = "virtual_account_name", length = 255)
    private String virtualAccountName; // Tên tài khoản ảo

    @Column(name = "virtual_account_number", length = 50)
    private String virtualAccountNumber; // Số tài khoản ảo


    @OneToOne(mappedBy = "payOSPayment", cascade = CascadeType.ALL)
    private IncomeTransaction incomeTransaction;
}

