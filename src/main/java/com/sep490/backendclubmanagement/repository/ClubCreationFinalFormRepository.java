package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubCreationFinalForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubCreationFinalFormRepository extends JpaRepository<ClubCreationFinalForm, Long> {
    Optional<ClubCreationFinalForm> findByRequestEstablishmentId(Long requestEstablishmentId);

    List<ClubCreationFinalForm> findAllByRequestEstablishmentIdOrderByCreatedAtDesc(Long requestEstablishmentId);

    Optional<ClubCreationFinalForm> findFirstByRequestEstablishmentIdOrderByCreatedAtDesc(Long requestEstablishmentId);
}

