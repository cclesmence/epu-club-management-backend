package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.RequestEstablishment;
import com.sep490.backendclubmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_creation_final_forms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubCreationFinalForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData; // JSON hoặc structured data

    @Column(name = "status", length = 50)
    private String status; // SUBMITTED, REVIEWED, APPROVED

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy; // SV điền form

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // CB/Admin xem xét

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_establishment_id", nullable = false)
    private RequestEstablishment requestEstablishment;
}



