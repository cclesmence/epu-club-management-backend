package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.event.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    EventType findByTypeName(String typeName);
}

