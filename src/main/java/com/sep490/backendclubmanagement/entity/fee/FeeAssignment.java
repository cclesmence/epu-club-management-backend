package com.sep490.backendclubmanagement.entity.fee;

import com.sep490.backendclubmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fee_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private FeeAssignmentStatus status;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

}
