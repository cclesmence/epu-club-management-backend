package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.BatchMarkAttendanceRequest;
import com.sep490.backendclubmanagement.dto.request.EventRequest;
import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.dto.response.EventRegistrationDto;
import com.sep490.backendclubmanagement.dto.response.EventResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.event.EventAttendance;
import com.sep490.backendclubmanagement.entity.event.EventType;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.EventMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.event.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMediaRepository eventMediaRepository;
    @Mock private ClubMemberShipRepository clubMemberShipRepository;
    @Mock private EventAttendanceRepository eventAttendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventMapper eventMapper;
    @Mock private MessageSource messageSource;

    @InjectMocks
    private EventService eventService;

    private Event buildEvent(Long id, Club club, EventType type, boolean draft) {
        return Event.builder()
                .id(id)
                .title("Event " + id)
                .description("Desc")
                .location("Hall")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .club(club)
                .eventType(type)
                .isDraft(draft)
                .build();
    }

    private EventAttendance buildAttendance(Event event, User user) {
        return EventAttendance.builder()
                .id(1L)
                .event(event)
                .user(user)
                .attendanceStatus(AttendanceStatus.REGISTERED)
                .build();
    }

    // -------- getAllEventsByFilter --------

    @Test
    void getAllEventsByFilter_shouldApplyKeywordFilter() {
        EventRequest req = new EventRequest();
        req.setKeyword("Demo");
        req.setPage(1);
        req.setSize(10);

        Event event = buildEvent(1L, null, null, false);
        event.setTitle("Demo Event");
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventRepository.getAllByFilter(eq(req), any(PageRequest.class))).thenReturn(page);
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).title("Event Demo").build());
        when(eventMediaRepository.findByEventIdInOrderByDisplayOrder(anyList())).thenReturn(List.of());

        EventResponse response = eventService.getAllEventsByFilter(req);

        assertEquals(1, response.getCount());
        verify(eventRepository).getAllByFilter(eq(req), eq(req.getPageable()));
    }

    // -------- getEventById --------

    @Test
    void getEventById_whenFound_shouldReturnDto() {
        Event event = buildEvent(1L, null, null, false);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).build());
        when(eventMediaRepository.findByEventIdOrderByDisplayOrder(1L)).thenReturn(List.of());

        EventData dto = eventService.getEventById(1L);

        assertEquals(1L, dto.getId());
    }

    @Test
    void getEventById_whenNotFound_shouldThrow() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.getEventById(1L));
    }

    // -------- getEventsByClubId --------

    @Test
    void getEventsByClubId_whenMember_shouldReturnEvents() {
        Club club = Club.builder().id(5L).build();
        Event event = buildEvent(1L, club, null, false);
        when(clubMemberShipRepository.existsByClubIdAndUserIdAndStatusActive(5L, 9L)).thenReturn(true);
        when(eventRepository.findByClubIdAndIsDraftFalse(5L)).thenReturn(List.of(event));
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).clubId(5L).build());
        when(eventMediaRepository.findByEventIdInOrderByDisplayOrder(anyList())).thenReturn(List.of());

        List<EventData> result = eventService.getEventsByClubId(5L, 9L, null, null);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getClubId());
    }

    @Test
    void getEventsByClubId_whenNotMember_shouldThrow() {
        when(clubMemberShipRepository.existsByClubIdAndUserIdAndStatusActive(5L, 9L)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> eventService.getEventsByClubId(5L, 9L, null, null));
    }

    // -------- register/cancel --------

    @Test
    void registerForEvent_whenValid_shouldSaveAttendance() {
        Event event = buildEvent(1L, null, EventType.builder().typeName("WORKSHOP").build(), false);
        User user = User.builder().id(9L).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(eventAttendanceRepository.existsByEventIdAndUserId(1L, 9L)).thenReturn(false);

        eventService.registerForEvent(1L, 9L);

        ArgumentCaptor<EventAttendance> captor = ArgumentCaptor.forClass(EventAttendance.class);
        verify(eventAttendanceRepository).save(captor.capture());
        assertEquals(AttendanceStatus.REGISTERED, captor.getValue().getAttendanceStatus());
    }

    @Test
    void registerForEvent_whenDraft_shouldThrow() {
        Event event = buildEvent(1L, null, EventType.builder().typeName("WORKSHOP").build(), true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(RuntimeException.class,
                () -> eventService.registerForEvent(1L, 9L));
    }

    @Test
    void registerForEvent_whenEventStartedAndNotMeeting_shouldThrow() {
        Event event = Event.builder()
                .id(1L)
                .title("Past")
                .eventType(EventType.builder().typeName("WORKSHOP").build())
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .isDraft(false)
                .build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(RuntimeException.class,
                () -> eventService.registerForEvent(1L, 9L));
    }

    @Test
    void cancelEventRegistration_whenValid_shouldDeleteAttendance() {
        Event event = buildEvent(1L, null, null, false);
        EventAttendance attendance = buildAttendance(event, User.builder().id(9L).build());
        when(eventAttendanceRepository.findByEventIdAndUserId(1L, 9L)).thenReturn(Optional.of(attendance));

        eventService.cancelEventRegistration(1L, 9L);

        verify(eventAttendanceRepository).delete(attendance);
    }

    @Test
    void cancelEventRegistration_whenNotRegistered_shouldThrow() {
        when(eventAttendanceRepository.findByEventIdAndUserId(1L, 9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.cancelEventRegistration(1L, 9L));
    }

    // -------- batchMarkAttendance --------

    @Test
    void batchMarkAttendance_whenValid_shouldUpdateStatuses() {
        Club club = Club.builder().id(5L).build();
        Event event = buildEvent(1L, club, null, false);
        event.setEndTime(LocalDateTime.now());
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventAttendance attendance = buildAttendance(event, User.builder().id(9L).build());
        when(eventAttendanceRepository.findByEventIdAndUserId(1L, 9L)).thenReturn(Optional.of(attendance));

        BatchMarkAttendanceRequest.AttendanceItem item = new BatchMarkAttendanceRequest.AttendanceItem();
        item.setUserId(9L);
        item.setAttendanceStatus(AttendanceStatus.PRESENT);
        BatchMarkAttendanceRequest request = new BatchMarkAttendanceRequest();
        request.setEventId(1L);
        request.setAttendances(List.of(item));

        eventService.batchMarkAttendance(1L, request.getAttendances());

        assertEquals(AttendanceStatus.PRESENT, attendance.getAttendanceStatus());
        verify(eventAttendanceRepository).saveAll(anyList());
    }

    @Test
    void batchMarkAttendance_whenInvalidStatus_shouldThrow() {
        Club club = Club.builder().id(5L).build();
        Event event = buildEvent(1L, club, null, false);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        BatchMarkAttendanceRequest.AttendanceItem item = new BatchMarkAttendanceRequest.AttendanceItem();
        item.setUserId(9L);
        item.setAttendanceStatus(AttendanceStatus.REGISTERED);

        BatchMarkAttendanceRequest request = new BatchMarkAttendanceRequest();
        request.setAttendances(List.of(item));

        assertThrows(RuntimeException.class,
                () -> eventService.batchMarkAttendance(1L, request.getAttendances()));
    }

    // -------- getEventRegistrations --------

    @Test
    void getEventRegistrations_shouldReturnList() {
        Event event = buildEvent(1L, null, null, false);
        EventAttendance attendance = buildAttendance(event, User.builder()
                .id(9L).fullName("A").studentCode("SE1").email("a@fpt.edu.vn").build());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventAttendanceRepository.findByEventId(1L)).thenReturn(List.of(attendance));

        List<EventRegistrationDto> list = eventService.getEventRegistrations(1L);

        assertEquals(1, list.size());
        assertEquals("A", list.get(0).getFullName());
    }

    @Test
    void getEventRegistrations_whenEventNotFound_shouldThrow() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.getEventRegistrations(1L));
    }

    // -------- getEventRegistrationCount --------

    @Test
    void getEventRegistrationCount_shouldReturnRepositoryValue() {
        when(eventAttendanceRepository.countByEventIdAndStatus(1L)).thenReturn(5L);

        assertEquals(5L, eventService.getEventRegistrationCount(1L));
    }
}

