package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.dto.request.EventRequest;
import com.sep490.backendclubmanagement.dto.response.EventWithoutReportRequirementDto;
import com.sep490.backendclubmanagement.dto.response.UpcomingEventDTO;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.event.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT new com.sep490.backendclubmanagement.dto.response.UpcomingEventDTO(e.id, e.title, e.startTime, e.location, c.clubName, m.mediaUrl) " +
            "FROM Event e JOIN e.club c LEFT JOIN e.eventMedia m " +
            "WHERE e.startTime > :now AND (m IS NULL OR m.displayOrder = 1) " +
            "ORDER BY e.startTime ASC")
    List<UpcomingEventDTO> findUpcomingEvents(LocalDateTime now, Pageable pageable);


    // ✅ Thêm phần lọc/tìm kiếm sự kiện (được gọi trong EventService)
    @Query(value = """
                                     SELECT DISTINCT e.*
                                         FROM events e
                                         LEFT JOIN clubs c ON e.club_id = c.id
                                         LEFT JOIN event_types et ON e.event_type_id = et.id
                                         LEFT JOIN event_media em ON em.event_id = e.id
                                         WHERE\s
                                             
                                              (:#{#request.eventTypeId} IS NULL OR e.event_type_id = :#{#request.eventTypeId})
                                             AND (:#{#request.clubId} IS NULL OR e.club_id = :#{#request.clubId})
                                             AND (
                                                 :#{#request.startTime} IS NULL\s
                                                 OR :#{#request.endTime} IS NULL\s
                                                 OR (e.end_time >= :#{#request.startTime} AND e.start_time <= :#{#request.endTime})
                                             )
                                            AND e.is_draft = false
                                            AND (et.type_name IS NULL OR UPPER(TRIM(et.type_name)) <> 'MEETING')
          """, nativeQuery = true,countProjection = "e.id")
    public Page<Event> getAllByFilter(EventRequest request, Pageable pageable);

    @Query("SELECT et FROM EventType et")
    List<EventType> findAllEventTypes();

    @Query("SELECT c FROM Club c")
    List<Club> findAllClubs();

    @Query("SELECT e FROM Event e WHERE (e.club.id = :clubId OR e.club.id IS NULL ) AND e.isDraft = false")
    List<Event> findByClubIdAndIsDraftFalse(Long clubId);

    @Query("SELECT e FROM Event e WHERE e.isDraft = false")
    List<Event> findByIsDraftFalse();

    /**
     * Lấy events theo clubId, không draft, giao với khoảng thời gian chỉ định
     */
    @Query("SELECT e FROM Event e " +
           "WHERE (e.club.id = :clubId OR e.club.id IS NULL) " +
           "AND e.isDraft = false " +
           "AND (:startTime IS NULL OR :endTime IS NULL OR (e.endTime >= :startTime AND e.startTime <= :endTime))")
    List<Event> findByClubIdAndIsDraftFalseInRange(Long clubId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Lấy tất cả events cho Staff (không bao gồm MEETING)
     */
    @Query("SELECT e FROM Event e WHERE e.isDraft = false " +
           "AND (e.eventType IS NULL OR UPPER(TRIM(e.eventType.typeName)) <> 'MEETING')")
    List<Event> findStaffAllEventsExcludingMeeting();

    /**
     * Lấy tất cả events cho Staff (không bao gồm MEETING) theo khoảng thời gian
     */
    @Query("SELECT e FROM Event e WHERE e.isDraft = false " +
           "AND (e.eventType IS NULL OR UPPER(TRIM(e.eventType.typeName)) <> 'MEETING') " +
           "AND (:startTime IS NULL OR :endTime IS NULL OR (e.endTime >= :startTime AND e.startTime <= :endTime))")
    List<Event> findStaffAllEventsExcludingMeetingInRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Lấy events theo clubId cho Staff (không bao gồm MEETING)
     */
    @Query("SELECT e FROM Event e WHERE (e.club.id = :clubId OR e.club.id IS NULL) " +
           "AND e.isDraft = false " +
           "AND (e.eventType IS NULL OR UPPER(TRIM(e.eventType.typeName)) <> 'MEETING')")
    List<Event> findStaffEventsByClubIdExcludingMeeting(Long clubId);

    /**
     * Lấy events theo clubId cho Staff (không bao gồm MEETING) theo khoảng thời gian
     */
    @Query("SELECT e FROM Event e WHERE (e.club.id = :clubId OR e.club.id IS NULL) " +
           "AND e.isDraft = false " +
           "AND (e.eventType IS NULL OR UPPER(TRIM(e.eventType.typeName)) <> 'MEETING') " +
           "AND (:startTime IS NULL OR :endTime IS NULL OR (e.endTime >= :startTime AND e.startTime <= :endTime))")
    List<Event> findStaffEventsByClubIdExcludingMeetingInRange(Long clubId, LocalDateTime startTime, LocalDateTime endTime);

    // STAFF: các sự kiện CLB đã hủy (isDraft = true, không còn chờ duyệt)
    @Query("SELECT e FROM Event e LEFT JOIN RequestEvent re ON re.event = e AND re.status IN :pendingStatuses " +
           "WHERE e.isDraft = true AND e.club IS NOT NULL AND re.id IS NULL")
    List<Event> findCancelledByStaffExcludingPending(java.util.List<com.sep490.backendclubmanagement.entity.RequestStatus> pendingStatuses);

    @Query("SELECT e FROM Event e LEFT JOIN RequestEvent re ON re.event = e AND re.status IN :pendingStatuses " +
           "WHERE e.isDraft = true AND e.club.id = :clubId AND re.id IS NULL")
    List<Event> findCancelledByStaffAndClubIdExcludingPending(Long clubId, java.util.List<com.sep490.backendclubmanagement.entity.RequestStatus> pendingStatuses);

    /**
     * Lấy draft events không có club (toàn trường) - cho STAFF
     */
    @Query("SELECT e FROM Event e WHERE e.isDraft = true AND e.club IS NULL")
    List<Event> findByIsDraftTrueAndClubIsNull();

    /**
     * Lấy danh sách events chưa được yêu cầu nộp báo cáo
     * Event chưa có yêu cầu báo cáo khi:
     * - Event có club (không null)
     * - Event không phải draft
     * - Không tồn tại SubmissionReportRequirement với ClubReportRequirement cho club tổ chức sự kiện
     */
    @Query("SELECT new com.sep490.backendclubmanagement.dto.response.EventWithoutReportRequirementDto(" +
            "e.id, e.title, c.id, c.clubName) " +
            "FROM Event e " +
            "JOIN e.club c " +
            "WHERE e.club IS NOT NULL " +
            "AND e.isDraft = false " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM SubmissionReportRequirement srr " +
            "    JOIN ClubReportRequirement crr ON crr.submissionReportRequirement.id = srr.id " +
            "    WHERE srr.event.id = e.id AND crr.club.id = c.id" +
            ") " +
            "ORDER BY e.id DESC")
    List<EventWithoutReportRequirementDto> findEventsWithoutReportRequirement();

    /**
     * Lấy danh sách events đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    @Query("SELECT e FROM Event e " +
           "WHERE e.club.id = :clubId " +
           "AND e.isDraft = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.startTime DESC")
    Page<Event> findPublishedEventsByClubId(Long clubId, String keyword, Pageable pageable);
}
