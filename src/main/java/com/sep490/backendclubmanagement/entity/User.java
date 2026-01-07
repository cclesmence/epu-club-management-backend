package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubCreationWorkFlowHistory;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.event.EventAttendance;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "student_code", unique = true, length = 50)
    private String studentCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "system_role_id")
    private SystemRole systemRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<ClubMemberShip> clubMemberships;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<Post> posts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Comment> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Like> likes;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<EventAttendance> eventAttendances;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<RequestEstablishment> requestEstablishments;

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL)
    private Set<RecruitmentApplication> recruitmentApplications;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<Report> reports;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<RequestNews> createdRequestNews;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<SubmissionReportRequirement> submissionReportRequirements;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private Set<RequestEvent> createdRequestEvents;

    @OneToMany(mappedBy = "actedBy", cascade = CascadeType.ALL)
    private Set<ClubCreationWorkFlowHistory> clubCreationWorkFlowHistories;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<IncomeTransaction> incomeTransactions;

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL)
    private Set<Notification> receivedNotifications;
}

