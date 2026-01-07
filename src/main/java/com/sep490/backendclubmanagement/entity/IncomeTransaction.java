package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "income_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, length = 100)
    private String reference;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "source", length = 200)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private TransactionStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_wallet_id", nullable = false)
    private ClubWallet clubWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id")
    private Fee fee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payos_payment_id")
    private PayOSPayment payOSPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}

