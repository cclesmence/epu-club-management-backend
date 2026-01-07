package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubProposalRepository extends JpaRepository<ClubProposal, Long> {
    Optional<ClubProposal> findByRequestEstablishmentId(Long requestEstablishmentId);
    
    List<ClubProposal> findAllByRequestEstablishmentIdOrderByCreatedAtDesc(Long requestEstablishmentId);
    
    Optional<ClubProposal> findByIdAndRequestEstablishmentId(Long proposalId, Long requestEstablishmentId);
    
    List<ClubProposal> findByRequestEstablishmentIdInOrderByCreatedAtDesc(List<Long> requestIds);
}

