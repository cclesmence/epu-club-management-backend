package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateEventRequest;
import com.sep490.backendclubmanagement.dto.request.EventApprovalRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateEventRequest;
import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.dto.response.MyDraftEventDto;
import com.sep490.backendclubmanagement.dto.response.PendingRequestDto;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.event.EventType;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.mapper.EventMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.event.EventManagementService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventManagementServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private RequestEventRepository requestEventRepository;
    @Mock private RoleService roleService;
    @Mock private UserRepository userRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private EventTypeRepository eventTypeRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventMediaRepository eventMediaRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private WebSocketService webSocketService;
    @Mock private NotificationService notificationService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private EventManagementService eventManagementService;

    private EventType meetingType;
    private EventType workshopType;
    private User staffUser;
    private User officerUser;
    private Club club;

    @BeforeEach
    void setUp() {
        meetingType = EventType.builder().id(1L).typeName("MEETING").build();
        workshopType = EventType.builder().id(2L).typeName("WORKSHOP").build();
        staffUser = User.builder().id(10L).fullName("Staff").email("staff@fpt.edu.vn").build();
        officerUser = User.builder().id(20L).fullName("Officer").email("officer@fpt.edu.vn").build();
        club = Club.builder().id(100L).clubName("Test Club").build();
    }

    private CreateEventRequest buildCreateReq(Long clubId, Long typeId, LocalDateTime start) {
        CreateEventRequest req = new CreateEventRequest();
        req.setClubId(clubId);
        req.setEventTypeId(typeId);
        req.setTitle("Demo Event");
        req.setDescription("Desc");
        req.setLocation("Hall");
        req.setStartTime(start);
        req.setEndTime(start.plusHours(2));
        req.setMediaFiles(new ArrayList<>());
        return req;
    }

    private Event buildEvent(Long id, Club club, EventType type, boolean draft) {
        return Event.builder()
                .id(id)
                .title("Demo")
                .description("Desc")
                .location("Hall")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .club(club)
                .eventType(type)
                .isDraft(draft)
                .build();
    }

    private RequestEvent buildRequestEvent(Long id, Event event, User creator, RequestStatus status) {
        return RequestEvent.builder()
                .id(id)
                .requestTitle(event.getTitle())
                .status(status)
                .requestDate(LocalDateTime.now())
                .event(event)
                .createdBy(creator)
                .build();
    }

    // -------- createEvent --------

    @Test
    void createEvent_whenStaffNonMeeting_shouldSaveDraft() {
        CreateEventRequest req = buildCreateReq(null, workshopType.getId(), LocalDateTime.now().plusDays(1));

        when(roleService.isStaff(10L)).thenReturn(true);
        when(roleService.canCreateEvent(10L, null)).thenReturn(true);
        when(eventTypeRepository.findById(workshopType.getId())).thenReturn(Optional.of(workshopType));
        when(userRepository.findById(10L)).thenReturn(Optional.of(staffUser));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(eventMapper.toDto(any(Event.class))).thenReturn(EventData.builder().id(1L).build());

        EventData dto = eventManagementService.createEvent(req, 10L);

        assertNotNull(dto);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getIsDraft()));
        verifyNoInteractions(requestEventRepository);
    }

    @Test
    void createEvent_whenStaffMeeting_shouldThrowForbidden() {
        CreateEventRequest req = buildCreateReq(null, meetingType.getId(), LocalDateTime.now().plusDays(1));
        when(roleService.isStaff(10L)).thenReturn(true);
        when(roleService.canCreateEvent(10L, null)).thenReturn(true);
        when(eventTypeRepository.findById(meetingType.getId())).thenReturn(Optional.of(meetingType));
        when(userRepository.findById(10L)).thenReturn(Optional.of(staffUser));

        assertThrows(ForbiddenException.class,
                () -> eventManagementService.createEvent(req, 10L));
    }

    @Test
    void createEvent_whenClubPresident_shouldCreatePendingUniversityRequest() {
        CreateEventRequest req = buildCreateReq(club.getId(), workshopType.getId(), LocalDateTime.now().plusDays(1));

        when(roleService.isStaff(20L)).thenReturn(false);
        when(roleService.canCreateEvent(20L, club.getId())).thenReturn(true);
        when(roleService.isClubPresident(20L, club.getId())).thenReturn(true);
        when(roleService.isClubOfficer(20L, club.getId())).thenReturn(false);
        when(eventTypeRepository.findById(workshopType.getId())).thenReturn(Optional.of(workshopType));
        when(userRepository.findById(20L)).thenReturn(Optional.of(officerUser));
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(50L);
            return e;
        });
        when(requestEventRepository.save(any(RequestEvent.class))).thenAnswer(invocation -> {
            RequestEvent re = invocation.getArgument(0);
            re.setId(60L);
            return re;
        });
        when(eventMapper.toDto(any(Event.class))).thenReturn(EventData.builder().id(50L).build());
        when(userRepository.findBySystemRole_RoleNameIgnoreCase("STAFF")).thenReturn(List.of());

        EventData dto = eventManagementService.createEvent(req, 20L);

        assertEquals(50L, dto.getId());
        ArgumentCaptor<RequestEvent> captor = ArgumentCaptor.forClass(RequestEvent.class);
        verify(requestEventRepository, atLeastOnce()).save(captor.capture());
        assertEquals(RequestStatus.PENDING_UNIVERSITY, captor.getValue().getStatus());
    }

    @Test
    void createEvent_whenNoPermission_shouldThrow() {
        CreateEventRequest req = buildCreateReq(club.getId(), workshopType.getId(), LocalDateTime.now().plusDays(1));
        when(roleService.canCreateEvent(30L, club.getId())).thenReturn(false);
        when(roleService.isStaff(30L)).thenReturn(false);
        when(userRepository.findById(30L)).thenReturn(Optional.of(User.builder().id(30L).build()));
        when(eventTypeRepository.findById(workshopType.getId())).thenReturn(Optional.of(workshopType));

        assertThrows(ForbiddenException.class,
                () -> eventManagementService.createEvent(req, 30L));
    }

    // -------- approveEventByClub --------

    @Test
    void approveEventByClub_whenApprove_shouldUpdateStatus() {
        Event event = buildEvent(1L, club, workshopType, true);
        RequestEvent requestEvent = buildRequestEvent(2L, event, officerUser, RequestStatus.PENDING_CLUB);
        EventApprovalRequest request = new EventApprovalRequest();
        request.setRequestEventId(requestEvent.getId());
        request.setStatus(RequestStatus.APPROVED_CLUB);

        when(requestEventRepository.findByIdWithEventAndClub(requestEvent.getId()))
                .thenReturn(Optional.of(requestEvent));
        when(roleService.isClubPresident(40L, club.getId())).thenReturn(true);
        when(userRepository.findById(40L)).thenReturn(Optional.of(User.builder().id(40L).fullName("Leader").build()));

        eventManagementService.approveEventByClub(request, 40L);

        assertEquals(RequestStatus.PENDING_UNIVERSITY, requestEvent.getStatus());
        verify(requestEventRepository, atLeastOnce()).save(requestEvent);
    }

    @Test
    void approveEventByClub_whenReject_shouldSetRejectedStatus() {
        Event event = buildEvent(1L, club, workshopType, true);
        RequestEvent requestEvent = buildRequestEvent(2L, event, officerUser, RequestStatus.PENDING_CLUB);
        EventApprovalRequest request = new EventApprovalRequest();
        request.setRequestEventId(requestEvent.getId());
        request.setStatus(RequestStatus.REJECTED_CLUB);

        when(requestEventRepository.findByIdWithEventAndClub(requestEvent.getId()))
                .thenReturn(Optional.of(requestEvent));
        when(roleService.isClubPresident(40L, club.getId())).thenReturn(true);
        when(userRepository.findById(40L)).thenReturn(Optional.of(User.builder().id(40L).fullName("Leader").build()));

        eventManagementService.approveEventByClub(request, 40L);

        assertEquals(RequestStatus.REJECTED_CLUB, requestEvent.getStatus());
    }

    @Test
    void approveEventByClub_whenStatusInvalid_shouldThrow() {
        Event event = buildEvent(1L, club, workshopType, true);
        RequestEvent requestEvent = buildRequestEvent(2L, event, officerUser, RequestStatus.PENDING_UNIVERSITY);
        EventApprovalRequest request = new EventApprovalRequest();
        request.setRequestEventId(requestEvent.getId());
        request.setStatus(RequestStatus.APPROVED_CLUB);

        when(requestEventRepository.findByIdWithEventAndClub(requestEvent.getId()))
                .thenReturn(Optional.of(requestEvent));
        when(roleService.isClubPresident(40L, club.getId())).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> eventManagementService.approveEventByClub(request, 40L));
    }

    // -------- approveEventByStaff --------

    @Test
    void approveEventByStaff_whenApprove_shouldPublishEvent() {
        Event event = buildEvent(1L, club, workshopType, true);
        RequestEvent requestEvent = buildRequestEvent(2L, event, officerUser, RequestStatus.PENDING_UNIVERSITY);
        EventApprovalRequest request = new EventApprovalRequest();
        request.setRequestEventId(2L);
        request.setStatus(RequestStatus.APPROVED_UNIVERSITY);

        when(requestEventRepository.findByIdWithEventAndClub(2L)).thenReturn(Optional.of(requestEvent));
        when(roleService.isStaff(10L)).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(staffUser));

        eventManagementService.approveEventByStaff(request, 10L);

        assertEquals(RequestStatus.APPROVED_UNIVERSITY, requestEvent.getStatus());
        assertFalse(event.getIsDraft());
        verify(eventRepository).save(event);
    }

    @Test
    void approveEventByStaff_whenNotStaff_shouldThrow() {
        EventApprovalRequest request = new EventApprovalRequest();
        request.setRequestEventId(3L);
        when(roleService.isStaff(10L)).thenReturn(false);
        when(requestEventRepository.findByIdWithEventAndClub(3L))
                .thenReturn(Optional.of(buildRequestEvent(3L, buildEvent(1L, club, workshopType, true), officerUser, RequestStatus.PENDING_UNIVERSITY)));

        assertThrows(ForbiddenException.class,
                () -> eventManagementService.approveEventByStaff(request, 10L));
    }

    // -------- getPendingRequests --------

    @Test
    void getPendingRequests_whenStaff_shouldReturnUniversityRequests() {
        RequestEvent re = buildRequestEvent(1L, buildEvent(1L, club, workshopType, true), officerUser, RequestStatus.PENDING_UNIVERSITY);
        when(roleService.isStaff(10L)).thenReturn(true);
        when(requestEventRepository.findAllByStatusWithAll(RequestStatus.PENDING_UNIVERSITY))
                .thenReturn(List.of(re));

        List<PendingRequestDto> result = eventManagementService.getPendingRequests(10L, null);

        assertEquals(1, result.size());
        assertEquals(re.getId(), result.get(0).getRequestEventId());
    }

    @Test
    void getPendingRequests_whenClubPresidentWithClub_shouldReturnClubRequests() {
        RequestEvent re = buildRequestEvent(1L, buildEvent(1L, club, workshopType, true), officerUser, RequestStatus.PENDING_CLUB);
        when(roleService.isStaff(20L)).thenReturn(false);
        when(roleService.isClubPresident(20L, club.getId())).thenReturn(true);
        when(requestEventRepository.findAllByStatusAndClubIdWithAll(RequestStatus.PENDING_CLUB, club.getId()))
                .thenReturn(List.of(re));

        List<PendingRequestDto> result = eventManagementService.getPendingRequests(20L, club.getId());

        assertEquals(1, result.size());
        assertEquals(RequestStatus.PENDING_CLUB, result.get(0).getStatus());
    }

    // -------- getMyDraftEvents --------

    @Test
    void getMyDraftEvents_whenStaff_shouldReturnDrafts() {
        Event event = buildEvent(1L, null, workshopType, true);
        when(roleService.isStaff(10L)).thenReturn(true);
        when(eventRepository.findByIsDraftTrueAndClubIsNull()).thenReturn(List.of(event));
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).build());

        List<MyDraftEventDto> result = eventManagementService.getMyDraftEvents(10L, null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getRequestStatus());
    }

    // -------- updateMyDraftEvent (staff draft) --------

    @Test
    void updateMyDraftEvent_whenStaffDraft_shouldUpdateFields() {
        Event event = buildEvent(1L, null, workshopType, true);
        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Updated");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(roleService.isStaff(10L)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).title("Updated").build());

        EventData dto = eventManagementService.updateMyDraftEvent(1L, req, 10L);

        assertEquals("Updated", dto.getTitle());
        verify(eventRepository).save(event);
    }

    // -------- deleteMyDraftEvent (staff) --------

    @Test
    void deleteMyDraftEvent_whenStaffDraft_shouldDelete() {
        Event event = buildEvent(1L, null, workshopType, true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(roleService.isStaff(10L)).thenReturn(true);

        eventManagementService.deleteMyDraftEvent(1L, 10L);

        verify(eventRepository).delete(event);
    }

    // -------- cancel/restore/publish --------

    @Test
    void cancelClubEventByStaff_shouldSetDraft() throws Exception {
        Event event = buildEvent(1L, club, workshopType, false);
        when(roleService.isStaff(10L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(notificationService.getClubManagers(club.getId())).thenReturn(List.of());
        when(requestEventRepository.findByEventId(1L)).thenReturn(Optional.empty());

        eventManagementService.cancelClubEventByStaff(1L, 10L, "reason");

        assertTrue(event.getIsDraft());
        verify(eventRepository).save(event);
    }

    @Test
    void restoreCancelledEventByStaff_shouldPublishEvent() throws Exception {
        Event event = buildEvent(1L, club, workshopType, true);
        when(roleService.isStaff(10L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(notificationService.getClubManagers(club.getId())).thenReturn(List.of());
        when(requestEventRepository.findByEventId(1L)).thenReturn(Optional.empty());

        eventManagementService.restoreCancelledEventByStaff(1L, 10L);

        assertFalse(event.getIsDraft());
        verify(eventRepository).save(event);
    }

    @Test
    void publishEventByStaff_shouldSetDraftFalse() throws Exception {
        Event event = buildEvent(1L, null, workshopType, true);
        when(roleService.isStaff(10L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDto(event)).thenReturn(EventData.builder().id(1L).build());
        when(userRepository.findById(10L)).thenReturn(Optional.of(staffUser));
        when(userRepository.findBySystemRole_RoleNameIgnoreCase("STUDENT")).thenReturn(List.of());
        when(userRepository.findBySystemRole_RoleNameIgnoreCase("TEAM_OFFICER")).thenReturn(List.of());
        when(userRepository.findBySystemRole_RoleNameIgnoreCase("CLUB_OFFICER")).thenReturn(List.of());

        EventData dto = eventManagementService.publishEventByStaff(1L, 10L);

        assertFalse(event.getIsDraft());
        assertEquals(1L, dto.getId());
        verify(eventRepository).save(event);
    }
}

