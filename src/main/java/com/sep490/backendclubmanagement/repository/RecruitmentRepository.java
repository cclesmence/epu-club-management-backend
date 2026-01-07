package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.recruitment.Recruitment;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {
    Page<Recruitment> findByClub_Id(Long clubId, Pageable pageable);
    Page<Recruitment> findByClub_IdAndStatus(Long clubId, RecruitmentStatus status, Pageable pageable);
    List<Recruitment> findByClub_IdAndStatusAndIdNot(Long clubId, RecruitmentStatus status, Long excludeId);
    
    /**
     * Find recruitment by ID with club eagerly loaded to avoid N+1 query
     * @param id Recruitment ID
     * @return Optional recruitment with club
     */
    @Query("SELECT r FROM Recruitment r JOIN FETCH r.club WHERE r.id = :id")
    Optional<Recruitment> findByIdWithClub(@Param("id") Long id);

    // Search by keyword in title or description
    @Query("SELECT r FROM Recruitment r WHERE r.club.id = :clubId " +
           "AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Recruitment> searchByClubIdAndKeyword(@Param("clubId") Long clubId, 
                                                @Param("keyword") String keyword, 
                                                Pageable pageable);
    
    // Search by keyword and status
    @Query("SELECT r FROM Recruitment r WHERE r.club.id = :clubId " +
           "AND r.status = :status " +
           "AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Recruitment> searchByClubIdAndStatusAndKeyword(@Param("clubId") Long clubId, 
                                                         @Param("status") RecruitmentStatus status,
                                                         @Param("keyword") String keyword, 
                                                         Pageable pageable);

    // Bulk update to close expired recruitments whose endDate is before provided time and currently OPEN
    @Modifying
    @Query("UPDATE Recruitment r SET r.status = :newStatus WHERE r.endDate < :now AND r.status = :oldStatus")
    int closeExpiredRecruitments(@Param("newStatus") RecruitmentStatus newStatus,
                                 @Param("oldStatus") RecruitmentStatus oldStatus,
                                 @Param("now") LocalDateTime now);
}
