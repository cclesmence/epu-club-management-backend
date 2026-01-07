package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.RequestEvent;
import com.sep490.backendclubmanagement.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestEventRepository extends JpaRepository<RequestEvent, Long> {
    
    @Query(value = "SELECT * FROM request_events WHERE event_id = :eventId", nativeQuery = true)
    Optional<RequestEvent> findByEventId(@Param("eventId") Long eventId);
    
    @Query(value = "SELECT * FROM request_events WHERE status = :#{#status.name()} ORDER BY request_date DESC", nativeQuery = true)
    List<RequestEvent> findByStatus(@Param("status") RequestStatus status);
    
    @Query(value = "SELECT re.* FROM request_events re " +
           "INNER JOIN events e ON re.event_id = e.id " +
           "WHERE re.status = :#{#status.name()} AND e.club_id = :clubId " +
           "ORDER BY re.request_date DESC", nativeQuery = true)
    List<RequestEvent> findByStatusAndClubId(
            @Param("status") RequestStatus status,
            @Param("clubId") Long clubId
    );

    // Use JPQL for IN with enum list to avoid native binding issues
    @Query("SELECT re FROM RequestEvent re WHERE re.status IN :statuses ORDER BY re.requestDate DESC")
    List<RequestEvent> findByStatuses(@Param("statuses") List<RequestStatus> statuses);
    
    @Query("SELECT re FROM RequestEvent re " +
           "LEFT JOIN FETCH re.event e " +
           "LEFT JOIN FETCH e.club " +
           "WHERE re.id = :id")
    Optional<RequestEvent> findByIdWithEventAndClub(@Param("id") Long id);

    // Fetch list with relations to avoid LazyInitialization when mapping DTOs
    @Query("SELECT re FROM RequestEvent re " +
           "LEFT JOIN FETCH re.event e " +
           "LEFT JOIN FETCH e.club c " +
           "LEFT JOIN FETCH re.createdBy u " +
           "LEFT JOIN FETCH e.eventType et " +
           "WHERE re.status = :status")
    List<RequestEvent> findAllByStatusWithAll(@Param("status") RequestStatus status);

    @Query("SELECT re FROM RequestEvent re " +
           "JOIN FETCH re.event e " +
           "LEFT JOIN FETCH e.club c " +
           "LEFT JOIN FETCH re.createdBy u " +
           "LEFT JOIN FETCH e.eventType et " +
           "WHERE re.status = :status AND e.club.id = :clubId")
    List<RequestEvent> findAllByStatusAndClubIdWithAll(@Param("status") RequestStatus status,
                                                       @Param("clubId") Long clubId);

    @Query("SELECT re FROM RequestEvent re " +
           "JOIN FETCH re.event e " +
           "LEFT JOIN FETCH e.club c " +
           "LEFT JOIN FETCH re.createdBy u " +
           "LEFT JOIN FETCH e.eventType et " +
           "WHERE re.status IN :statuses AND e.club.id = :clubId")
    List<RequestEvent> findAllByStatusesAndClubIdWithAll(@Param("statuses") List<RequestStatus> statuses,
                                                         @Param("clubId") Long clubId);


    @Query("SELECT re.event.id FROM RequestEvent re WHERE re.createdBy.id = :userId AND re.status IN :statuses")
    List<Long> findEventIdsOfDraftByUser(@Param("userId") Long userId, @Param("statuses") List<RequestStatus> statuses);

    @Query("SELECT re FROM RequestEvent re WHERE re.createdBy.id = :userId AND re.status IN :statuses")
    List<RequestEvent> findByCreatedByIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<RequestStatus> statuses);

    @Query("SELECT re FROM RequestEvent re JOIN FETCH re.event e WHERE e.id = :eventId AND re.createdBy.id = :userId AND re.status IN :statuses")
    Optional<RequestEvent> findByEventIdAndCreatorWithEventAndStatusIn(@Param("eventId") Long eventId,
                                                                       @Param("userId") Long userId,
                                                                       @Param("statuses") List<RequestStatus> statuses);

    @Query("SELECT re FROM RequestEvent re JOIN FETCH re.event e " +
           "LEFT JOIN FETCH e.club c " +
           "LEFT JOIN FETCH re.createdBy u " +
           "LEFT JOIN FETCH e.eventType et " +
           "WHERE e.id = :eventId AND re.status IN :statuses AND e.club.id = :clubId")
    Optional<RequestEvent> findByEventIdAndStatusesAndClubIdWithAll(@Param("eventId") Long eventId,
                                                                      @Param("statuses") List<RequestStatus> statuses,
                                                                      @Param("clubId") Long clubId);

    @Query("SELECT re FROM RequestEvent re JOIN FETCH re.event e WHERE e.id = :eventId AND re.createdBy.id = :userId")
    Optional<RequestEvent> findByEventIdAndCreatedById(@Param("eventId") Long eventId,
                                                       @Param("userId") Long userId);
}

