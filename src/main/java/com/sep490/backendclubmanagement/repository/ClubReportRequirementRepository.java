package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubReportRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClubReportRequirementRepository extends JpaRepository<ClubReportRequirement, Long> {

    /**
     * Find ClubReportRequirement by clubId and submissionReportRequirementId
     * Fetch report if exists to avoid LazyInitializationException and check if report already exists
     */
    @Query("SELECT crr FROM ClubReportRequirement crr " +
           "LEFT JOIN FETCH crr.report " +
           "JOIN FETCH crr.club " +
           "JOIN FETCH crr.submissionReportRequirement " +
           "WHERE crr.club.id = :clubId " +
           "AND crr.submissionReportRequirement.id = :submissionReportRequirementId")
    Optional<ClubReportRequirement> findByClubIdAndSubmissionReportRequirementId(
            @Param("clubId") Long clubId,
            @Param("submissionReportRequirementId") Long submissionReportRequirementId
    );

    /**
     * Find all ClubReportRequirements by submissionReportRequirementId
     * Fetch club eagerly to avoid LazyInitializationException
     */
    @Query("SELECT crr FROM ClubReportRequirement crr " +
           "JOIN FETCH crr.club " +
           "WHERE crr.submissionReportRequirement.id = :submissionReportRequirementId")
    List<ClubReportRequirement> findBySubmissionReportRequirementId(
            @Param("submissionReportRequirementId") Long submissionReportRequirementId
    );

    /**
     * Find all ClubReportRequirements by multiple submissionReportRequirementIds
     * Fetch club and report eagerly to avoid N+1 queries and LazyInitializationException
     * Used for batch loading in getAllReportRequirements()
     */
    @Query("SELECT crr FROM ClubReportRequirement crr " +
           "JOIN FETCH crr.club " +
           "LEFT JOIN FETCH crr.report r " +
           "WHERE crr.submissionReportRequirement.id IN :submissionReportRequirementIds")
    List<ClubReportRequirement> findBySubmissionReportRequirementIdIn(
            @Param("submissionReportRequirementIds") List<Long> submissionReportRequirementIds
    );

    /**
     * Find all ClubReportRequirements by clubId
     * Fetch submissionReportRequirement eagerly to avoid LazyInitializationException
     * Ordered by creation time (newest first)
     */
    @Query("SELECT crr FROM ClubReportRequirement crr " +
           "JOIN FETCH crr.submissionReportRequirement " +
           "WHERE crr.club.id = :clubId " +
           "ORDER BY crr.submissionReportRequirement.createdAt DESC")
    List<ClubReportRequirement> findByClubId(
            @Param("clubId") Long clubId
    );

    /**
     * Find all ClubReportRequirements by clubId and teamId
     * Fetch submissionReportRequirement eagerly to avoid LazyInitializationException
     * Ordered by creation time (newest first)
     */
    @Query("SELECT crr FROM ClubReportRequirement crr " +
           "JOIN FETCH crr.submissionReportRequirement " +
           "WHERE crr.club.id = :clubId " +
           "AND crr.teamId = :teamId " +
           "ORDER BY crr.submissionReportRequirement.createdAt DESC")
    List<ClubReportRequirement> findByClubIdAndTeamId(
            @Param("clubId") Long clubId,
            @Param("teamId") Long teamId
    );

    /**
     * Find all ClubReportRequirements by clubId with filters and pagination
     * Filters:
     * - keyword: search in title and description
     * - status: filter by report status (if report exists) or UNSUBMITTED/OVERDUE
     * - semesterId: filter by semester (based on deadline date range)
     * - overdue: filter by deadline passed
     * Note: Status filtering for enum values is handled in service layer
     * Using separate count query to avoid issues with DISTINCT and FETCH in pagination
     */
    @Query(value = "SELECT crr FROM ClubReportRequirement crr " +
           "LEFT JOIN FETCH crr.report r " +
           "LEFT JOIN FETCH crr.submissionReportRequirement srr " +
           "LEFT JOIN FETCH srr.createdBy " +
           "WHERE crr.club.id = :clubId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(srr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(srr.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:semesterId IS NULL OR EXISTS " +
           "     (SELECT s FROM Semester s WHERE s.id = :semesterId " +
           "      AND srr.dueDate >= s.startDate " +
           "      AND srr.dueDate <= s.endDate)) " +
           "AND (:filterUnsubmitted IS NULL OR :filterUnsubmitted = false OR " +
           "     (:filterUnsubmitted = true AND r IS NULL)) " +
           "AND (:filterOverdue IS NULL OR :filterOverdue = false OR " +
           "     (:filterOverdue = true AND srr.dueDate < :currentDate)) " +
           "AND (:reportStatus IS NULL OR (r IS NOT NULL AND r.status = :reportStatus)) " +
           "AND (:teamId IS NULL OR crr.teamId = :teamId)",
           countQuery = "SELECT COUNT(crr) FROM ClubReportRequirement crr " +
           "LEFT JOIN crr.report r " +
           "LEFT JOIN crr.submissionReportRequirement srr " +
           "WHERE crr.club.id = :clubId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(srr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(srr.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:semesterId IS NULL OR EXISTS " +
           "     (SELECT s FROM Semester s WHERE s.id = :semesterId " +
           "      AND srr.dueDate >= s.startDate " +
           "      AND srr.dueDate <= s.endDate)) " +
           "AND (:filterUnsubmitted IS NULL OR :filterUnsubmitted = false OR " +
           "     (:filterUnsubmitted = true AND r IS NULL)) " +
           "AND (:filterOverdue IS NULL OR :filterOverdue = false OR " +
           "     (:filterOverdue = true AND srr.dueDate < :currentDate)) " +
           "AND (:reportStatus IS NULL OR (r IS NOT NULL AND r.status = :reportStatus)) " +
           "AND (:teamId IS NULL OR crr.teamId = :teamId)")
    Page<ClubReportRequirement> findByClubIdWithFilters(
            @Param("clubId") Long clubId,
            @Param("keyword") String keyword,
            @Param("filterUnsubmitted") Boolean filterUnsubmitted,
            @Param("filterOverdue") Boolean filterOverdue,
            @Param("reportStatus") com.sep490.backendclubmanagement.entity.ReportStatus reportStatus,
            @Param("semesterId") Long semesterId,
            @Param("teamId") Long teamId,
            @Param("currentDate") LocalDateTime currentDate,
            Pageable pageable
    );
}

