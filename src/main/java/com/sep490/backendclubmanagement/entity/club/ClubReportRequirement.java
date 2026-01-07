package com.sep490.backendclubmanagement.entity.club;

import com.sep490.backendclubmanagement.entity.BaseEntity;
import com.sep490.backendclubmanagement.entity.Report;
import com.sep490.backendclubmanagement.entity.SubmissionReportRequirement;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "club_report_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubReportRequirement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_report_requirement_id", nullable = false)
    private SubmissionReportRequirement submissionReportRequirement;

    @OneToOne(mappedBy = "clubReportRequirement", cascade = CascadeType.ALL)
    private Report report;

    @Column(name = "team_id", nullable = true)
    private Long teamId;
}
