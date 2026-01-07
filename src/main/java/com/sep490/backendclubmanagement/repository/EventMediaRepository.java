package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.event.EventMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface EventMediaRepository extends JpaRepository<EventMedia, Long> {

    @Query(value = """
             SELECT em.mediaUrl FROM EventMedia em WHERE em.event.id = :eventId ORDER BY em.displayOrder""",
           nativeQuery = false)
    List<String> findMediaUrlsByEventId(@Param("eventId") Long eventId);

    @Query(value = """
             SELECT em FROM EventMedia em WHERE em.event.id = :eventId ORDER BY em.displayOrder""",
           nativeQuery = false)
    List<EventMedia> findByEventIdOrderByDisplayOrder(@Param("eventId") Long eventId);

    @Query(value = """
             SELECT em FROM EventMedia em 
             WHERE em.event.id IN :eventIds 
             ORDER BY em.event.id, em.displayOrder
            """, nativeQuery = false)
    List<EventMedia> findByEventIdInOrderByDisplayOrder(@Param("eventIds") List<Long> eventIds);

    // Xóa tất cả media theo event id (để tránh lỗi FK)
    void deleteByEvent_Id(Long eventId);
}
