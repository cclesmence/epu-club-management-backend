package com.sep490.backendclubmanagement.entity.recruitment;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "recruitment_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "team_id")
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private RecruitmentApplicationStatus status;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    @Column(name = "interview_time")
    private LocalDateTime interviewTime;

    @Column(name = "interview_address", columnDefinition = "TEXT")
    private String interviewAddress;

    @Column(name = "interview_preparation_requirements", columnDefinition = "TEXT")
    private String interviewPreparationRequirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private Set<RecruitmentFormAnswer> answers;
}

