package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubCreationFinalForm;
import com.sep490.backendclubmanagement.entity.club.ClubCreationWorkFlowHistory;
import com.sep490.backendclubmanagement.entity.club.ClubProposal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "request_establishments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestEstablishment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 100)
    private String clubName;

    @Column(name = "club_category", nullable = false, length = 100)
    private String clubCategory;

    @Column(name = "club_code", length = 50)
    private String clubCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private RequestEstablishmentStatus status;

    @Column(name = "send_date")
    private LocalDateTime sendDate;

    @Column(name = "expected_member_count")
    private Integer expectedMemberCount;

    @Column(name = "activity_objectives", columnDefinition = "TEXT")
    private String activityObjectives; // Mục tiêu hoạt động

    @Column(name = "expected_activities", columnDefinition = "TEXT")
    private String expectedActivities; // Hoạt động dự kiến

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả CLB

    @Column(name = "email", length = 100)
    private String email; // Email liên hệ

    @Column(name = "phone", length = 20)
    private String phone; // Số điện thoại liên hệ

    @Column(name = "facebook_link", length = 255)
    private String facebookLink; // Link Facebook

    @Column(name = "instagram_link", length = 255)
    private String instagramLink; // Link Instagram

    @Column(name = "tiktok_link", length = 255)
    private String tiktokLink; // Link TikTok

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @OneToMany(mappedBy = "requestEstablishment", cascade = CascadeType.ALL)
    private Set<ClubProposal> clubProposals;

    @OneToMany(mappedBy = "requestEstablishment", cascade = CascadeType.ALL)
    private Set<ClubCreationWorkFlowHistory> workflowHistories;

    @OneToOne(mappedBy = "requestEstablishment", cascade = CascadeType.ALL)
    private DefenseSchedule defenseSchedule;

    @OneToOne(mappedBy = "requestEstablishment", cascade = CascadeType.ALL)
    private ClubCreationFinalForm finalForm;
}

