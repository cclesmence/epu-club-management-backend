package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "outcome_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutcomeTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", unique = true, length = 100)
    private String transactionCode;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "recipient", length = 200)
    private String recipient;

    @Column(name = "purpose", length = 200)
    private String purpose;

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
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}

