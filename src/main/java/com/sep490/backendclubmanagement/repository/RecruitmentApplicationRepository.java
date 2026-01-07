package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplication;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {
    Optional<RecruitmentApplication> findByApplicant_IdAndRecruitment_Id(Long applicantId, Long recruitmentId);
    
    /**
     * Find application by ID with recruitment, club, and applicant eagerly loaded
     * @param id Application ID
     * @return Optional application with relationships
     */
    @Query("SELECT ra FROM RecruitmentApplication ra " +
           "LEFT JOIN FETCH ra.recruitment r " +
           "LEFT JOIN FETCH r.club " +
           "LEFT JOIN FETCH ra.applicant " +
           "WHERE ra.id = :id")
    Optional<RecruitmentApplication> findByIdWithDetails(@Param("id") Long id);

    // Dynamic search for my applications - supports all combinations of parameters
    // Uses database function to handle Vietnamese text search efficiently
    @Query("SELECT ra FROM RecruitmentApplication ra " +
           "LEFT JOIN FETCH ra.recruitment r " +
           "LEFT JOIN FETCH r.club " +
           "WHERE ra.applicant.id = :applicantId " +
           "AND (:status IS NULL OR ra.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(REPLACE(REPLACE(ra.recruitment.title, 'đ', 'd'), 'Đ', 'd')) LIKE LOWER(CONCAT('%', REPLACE(REPLACE(:keyword, 'đ', 'd'), 'Đ', 'd'), '%')) OR " +
           "     LOWER(REPLACE(REPLACE(ra.recruitment.club.clubName, 'đ', 'd'), 'Đ', 'd')) LIKE LOWER(CONCAT('%', REPLACE(REPLACE(:keyword, 'đ', 'd'), 'Đ', 'd'), '%')))")
    Page<RecruitmentApplication> findMyApplications(@Param("applicantId") Long applicantId,
                                                     @Param("status") RecruitmentApplicationStatus status,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);

    // Dynamic search for applications by recruitment - supports all combinations of parameters
    // Uses database function to handle Vietnamese text search efficiently
    @Query("SELECT ra FROM RecruitmentApplication ra " +
           "LEFT JOIN FETCH ra.applicant " +
           "WHERE ra.recruitment.id = :recruitmentId " +
           "AND (:status IS NULL OR ra.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(REPLACE(REPLACE(ra.applicant.fullName, 'đ', 'd'), 'Đ', 'd')) LIKE LOWER(CONCAT('%', REPLACE(REPLACE(:keyword, 'đ', 'd'), 'Đ', 'd'), '%')) OR " +
           "     LOWER(REPLACE(REPLACE(ra.applicant.email, 'đ', 'd'), 'Đ', 'd')) LIKE LOWER(CONCAT('%', REPLACE(REPLACE(:keyword, 'đ', 'd'), 'Đ', 'd'), '%')) OR " +
           "     LOWER(REPLACE(REPLACE(ra.applicant.studentCode, 'đ', 'd'), 'Đ', 'd')) LIKE LOWER(CONCAT('%', REPLACE(REPLACE(:keyword, 'đ', 'd'), 'Đ', 'd'), '%')))")
    Page<RecruitmentApplication> findApplicationsByRecruitment(@Param("recruitmentId") Long recruitmentId,
                                                                @Param("status") RecruitmentApplicationStatus status,
                                                                @Param("keyword") String keyword,
                                                                Pageable pageable);
}
