package com.sep490.backendclubmanagement.entity.fee;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.IncomeTransaction;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.entity.club.Club;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_type", length = 50)
    @Enumerated(EnumType.STRING)
    private FeeType feeType; // MEMBERSHIP, EVENT, OTHER

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    @Column(name = "is_draft", nullable = false)
    @Builder.Default
    private Boolean isDraft = true;

    @Column(name = "has_ever_expired", nullable = false)
    @Builder.Default
    private Boolean hasEverExpired = false; // Once true, amount can never be edited

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @OneToMany(mappedBy = "fee", cascade = CascadeType.ALL)
    private Set<IncomeTransaction> incomeTransactions;
}

