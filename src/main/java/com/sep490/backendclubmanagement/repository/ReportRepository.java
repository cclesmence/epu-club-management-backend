package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.Report;
import com.sep490.backendclubmanagement.entity.ReportStatus;
import com.sep490.backendclubmanagement.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:clubId IS NULL OR r.clubReportRequirement.club.id = :clubId) " +
           "AND (:semesterId IS NULL OR r.semester.id = :semesterId) " +
           "AND (:reportType IS NULL OR r.clubReportRequirement.submissionReportRequirement.reportType = :reportType) " +
           "AND (:keyword IS NULL OR LOWER(r.reportTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.submittedDate DESC, r.createdAt DESC")
    Page<Report> findAllWithFilters(
            @Param("status") ReportStatus status,
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId,
            @Param("reportType") ReportType reportType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.clubReportRequirement crr " +
           "LEFT JOIN FETCH crr.club " +
           "LEFT JOIN FETCH crr.submissionReportRequirement srr " +
           "LEFT JOIN FETCH srr.createdBy " +
           "LEFT JOIN FETCH r.semester " +
           "LEFT JOIN FETCH r.createdBy " +
           "WHERE r.id = :id")
    Optional<Report> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT r FROM Report r " +
           "WHERE r.status = :status " +
           "ORDER BY r.submittedDate ASC")
    List<Report> findAllByStatus(@Param("status") ReportStatus status);

    @Query("SELECT r FROM Report r " +
           "WHERE r.clubReportRequirement.club.id = :clubId " +
           "ORDER BY r.submittedDate DESC")
    List<Report> findAllByClubId(@Param("clubId") Long clubId);

    @Query("SELECT r FROM Report r " +
           "WHERE r.clubReportRequirement.submissionReportRequirement.id = :requirementId")
    List<Report> findAllByReportRequirementId(@Param("requirementId") Long requirementId);

    @Query("SELECT r FROM Report r " +
            "WHERE r.status <> 'DRAFT' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:clubId IS NULL OR r.clubReportRequirement.club.id = :clubId) " +
            "AND (:semesterId IS NULL OR r.semester.id = :semesterId) " +
            "AND (:reportType IS NULL OR r.clubReportRequirement.submissionReportRequirement.reportType = :reportType) " +
            "AND (:keyword IS NULL OR LOWER(r.reportTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY r.submittedDate DESC, r.createdAt DESC")
    Page<Report> findByClubIdWithFilter(@Param("status") ReportStatus status,
                                        @Param("clubId") Long clubId,
                                        @Param("semesterId") Long semesterId,
                                        @Param("reportType") ReportType reportType,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    @Query("SELECT r FROM Report r " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "AND (:clubId IS NULL OR r.clubReportRequirement.club.id = :clubId) " +
            "AND (:semesterId IS NULL OR r.semester.id = :semesterId) " +
            "AND r.createdBy.id = :userId " +
            "AND (:reportType IS NULL OR r.clubReportRequirement.submissionReportRequirement.reportType = :reportType) " +
            "AND (:keyword IS NULL OR LOWER(r.reportTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY r.submittedDate DESC, r.createdAt DESC")
    Page<Report> findByClubIdAndUserIdWithFilter(@Param("status") ReportStatus status,
                                        @Param("clubId") Long clubId,
                                        @Param("semesterId") Long semesterId,
                                        @Param("reportType") ReportType reportType,
                                        @Param("keyword") String keyword,
                                        @Param("userId") Long userId,
                                        Pageable pageable);

    /**
     * Find report by clubId and reportRequirementId
     * Fetch all relations to avoid LazyInitializationException
     */
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.clubReportRequirement crr " +
           "LEFT JOIN FETCH crr.club " +
           "LEFT JOIN FETCH crr.submissionReportRequirement srr " +
           "LEFT JOIN FETCH srr.createdBy " +
           "LEFT JOIN FETCH r.semester " +
           "LEFT JOIN FETCH r.createdBy " +
           "WHERE r.clubReportRequirement.club.id = :clubId " +
           "AND r.clubReportRequirement.submissionReportRequirement.id = :requirementId")
    Optional<Report> findByClubIdAndReportRequirementId(
            @Param("clubId") Long clubId,
            @Param("requirementId") Long requirementId
    );
}

