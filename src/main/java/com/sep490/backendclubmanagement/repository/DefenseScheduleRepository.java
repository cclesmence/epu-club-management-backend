package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.DefenseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DefenseScheduleRepository extends JpaRepository<DefenseSchedule, Long> {
    Optional<DefenseSchedule> findByRequestEstablishmentId(Long requestEstablishmentId);
}

