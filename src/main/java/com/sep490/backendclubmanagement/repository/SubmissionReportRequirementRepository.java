package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.ReportType;
import com.sep490.backendclubmanagement.entity.SubmissionReportRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionReportRequirementRepository extends JpaRepository<SubmissionReportRequirement, Long> {
    
    /**
     * Find all submission report requirements with filters
     * @param reportType Filter by report type (optional)
     * @param clubId Filter by club ID through ClubReportRequirement (optional)
     * @param keyword Search in title and description (optional)
     * @param pageable Pagination
     * @return Page of SubmissionReportRequirement
     */
    @Query("SELECT DISTINCT srr FROM SubmissionReportRequirement srr " +
           "LEFT JOIN ClubReportRequirement crr ON crr.submissionReportRequirement.id = srr.id " +
           "WHERE " +
           "(:reportType IS NULL OR srr.reportType = :reportType) " +
           "AND (:clubId IS NULL OR crr.club.id = :clubId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(srr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(srr.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SubmissionReportRequirement> findAllWithFilters(
            @Param("reportType") ReportType reportType,
            @Param("clubId") Long clubId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

