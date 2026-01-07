package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.RequestEstablishment;
import com.sep490.backendclubmanagement.entity.RequestEstablishmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestEstablishmentRepository extends JpaRepository<RequestEstablishment, Long> {

    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    @Query("""
        SELECT r
        FROM RequestEstablishment r
        WHERE r.id = :id
    """)
    Optional<RequestEstablishment> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    @Query("""
        SELECT r
        FROM RequestEstablishment r
        WHERE r.createdBy.id = :userId
        ORDER BY r.createdAt DESC
    """)
    Page<RequestEstablishment> findByCreatedBy(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    @Query("""
        SELECT r
        FROM RequestEstablishment r
        WHERE r.createdBy.id = :userId
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.createdAt DESC
    """)
    Page<RequestEstablishment> findByCreatedByAndStatus(
            @Param("userId") Long userId,
            @Param("status") RequestEstablishmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    @Query("""
        SELECT r
        FROM RequestEstablishment r
        WHERE r.assignedStaff.id = :staffId
        AND r.status IN :statuses
        ORDER BY r.createdAt DESC
    """)
    Page<RequestEstablishment> findByAssignedStaffAndStatusIn(
            @Param("staffId") Long staffId,
            @Param("statuses") List<RequestEstablishmentStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    @Query("""
        SELECT r
        FROM RequestEstablishment r
        WHERE r.status IN :statuses
        ORDER BY r.createdAt DESC
    """)
    Page<RequestEstablishment> findByStatusIn(
            @Param("statuses") List<RequestEstablishmentStatus> statuses,
            Pageable pageable
    );

    boolean existsByClubCodeIgnoreCase(String clubCode);
    boolean existsByClubNameIgnoreCase(String clubName);
    boolean existsByClubNameIgnoreCaseAndIdNot(String clubName, Long id);
}

