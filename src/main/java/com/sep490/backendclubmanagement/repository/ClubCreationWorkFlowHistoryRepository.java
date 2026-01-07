package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubCreationWorkFlowHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubCreationWorkFlowHistoryRepository extends JpaRepository<ClubCreationWorkFlowHistory, Long> {
    
    @EntityGraph(attributePaths = {"actedBy", "clubCreationStep", "requestEstablishment"})
    @Query("""
        SELECT h
        FROM ClubCreationWorkFlowHistory h
        WHERE h.requestEstablishment.id = :requestId
        ORDER BY h.actionDate DESC, h.createdAt DESC
    """)
    Page<ClubCreationWorkFlowHistory> findByRequestEstablishmentId(
            @Param("requestId") Long requestId,
            Pageable pageable
    );
}

