package com.sep490.backendclubmanagement.entity;

import com.sep490.backendclubmanagement.entity.club.ClubReportRequirement;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_title", nullable = false, length = 500)
    private String reportTitle;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ReportStatus status;

    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    @Column(name = "reviewer_feedback", columnDefinition = "TEXT")
    private String reviewerFeedback;

    @Column(name = "must_resubmit")
    private boolean mustResubmit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy")
    private User createdBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_report_requirement_id", nullable = false, unique = true)
    private ClubReportRequirement clubReportRequirement;

}

