package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.config.EncryptedStringConverter;
import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.IncomeTransaction;
import com.sep490.backendclubmanagement.entity.OutcomeTransaction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "club_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubWallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, unique = true)
    private Club club;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_income", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_outcome", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalOutcome = BigDecimal.ZERO;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "payOs_client_id", length = 500)
    private String payOsClientId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "payOs_api_key", length = 500)
    private String payOsApiKey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "payOs_checksum_key", length = 500)
    private String payOsChecksumKey;

    @Column(name = "payOs_status", length = 100)
    private String payOsStatus;


    @OneToMany(mappedBy = "clubWallet", cascade = CascadeType.ALL)
    private Set<IncomeTransaction> incomeTransactions;

    @OneToMany(mappedBy = "clubWallet", cascade = CascadeType.ALL)
    private Set<OutcomeTransaction> outcomeTransactions;
}

