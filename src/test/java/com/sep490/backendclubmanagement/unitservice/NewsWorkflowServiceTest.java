package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.ApproveNewsRequest;
import com.sep490.backendclubmanagement.dto.request.CreateNewsRequest;
import com.sep490.backendclubmanagement.dto.request.RejectNewsRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.dto.response.NewsRequestResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.mapper.NewsMapper;
import com.sep490.backendclubmanagement.mapper.RequestNewsMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.news.NewsWorkflowService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class NewsWorkflowServiceTest {

    @Mock
    private RequestNewsRepository requestRepo;
    @Mock
    private NewsRepository newsRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ClubRepository clubRepo;
    @Mock
    private RoleGuard guard;
    @Mock
    private RequestNewsMapper mapper;
    @Mock
    private NewsMapper newsMapper;
    @Mock
    private TeamRepository teamRepo;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NewsWorkflowService newsWorkflowService;

    private User creator;
    private Club club;
    private User clubManager;
    private User staffUser;
    @BeforeEach
    void setup() {
        creator = new User();
        creator.setId(1L);
        creator.setFullName("Creator");

        clubManager = new User();
        clubManager.setId(2L);
        clubManager.setFullName("Manager");
        staffUser = new User();
        staffUser.setId(99L);
        staffUser.setFullName("Staff");

        club = new Club();
        club.setId(10L);
    }

    // ========== createRequest ==========

    @Test
    void createRequest_staffPendingUniversity_success() throws AppException {
        // Arrange
        Long me = creator.getId();
        Long clubId = club.getId();

        CreateNewsRequest req = new CreateNewsRequest();
        req.setTitle("Tiêu đề");
        req.setContent("Nội dung");
        req.setThumbnailUrl("thumb.png");
        req.setNewsType("GENERAL");
        req.setClubId(clubId);

        when(userRepo.findById(me)).thenReturn(Optional.of(creator));
        when(clubRepo.findById(clubId)).thenReturn(Optional.of(club));
        when(guard.isStaff(me)).thenReturn(true);
        when(guard.canApproveAtClub(me, clubId)).thenReturn(false);
        when(guard.isLead(me, clubId)).thenReturn(false);

        // mock save request
        MockitoAnswer<RequestNews> saveAnswer = new MockitoAnswer<>(r -> {
            RequestNews rn = r.getArgument(0);
            rn.setId(100L);
            rn.setStatus(RequestStatus.PENDING_UNIVERSITY);
            rn.setClub(club);
            rn.setCreatedBy(creator);
            return rn;
        });
        when(requestRepo.save(any(RequestNews.class))).thenAnswer(saveAnswer);

        RequestNews detail = new RequestNews();
        detail.setId(100L);
        detail.setStatus(RequestStatus.PENDING_UNIVERSITY);

        when(requestRepo.findDetailById(100L)).thenReturn(Optional.of(detail));

        NewsRequestResponse dto = NewsRequestResponse.builder()
                .id(100L)
                .status(RequestStatus.PENDING_UNIVERSITY)
                .build();
        when(mapper.toDto(detail)).thenReturn(dto);

        // Act
        NewsRequestResponse resp = newsWorkflowService.createRequest(me, req);

        // Assert
        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        assertEquals(RequestStatus.PENDING_UNIVERSITY, resp.getStatus());

        // broadcast phải gửi cho STAFF (system role)
        verify(webSocketService, times(1)).broadcastToSystemRole(
                eq("STAFF"),
                eq("NEWS_REQUEST"),
                eq("CREATED"),
                any(Map.class)
        );

        // vì là staff tự tạo → KHÔNG gửi noti staff lần 2
        verify(notificationService, never()).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
                any(), any(), any(), any()
        );
    }

    @Test
    void createRequest_leadWithoutTeam_throwsIllegalArgument() {
        // Arrange
        Long me = creator.getId();
        Long clubId = club.getId();

        CreateNewsRequest req = new CreateNewsRequest();
        req.setTitle("Tiêu đề");
        req.setContent("Nội dung");
        req.setClubId(clubId);
        req.setTeamId(null); // thiếu teamId

        when(userRepo.findById(me)).thenReturn(Optional.of(creator));
        when(clubRepo.findById(clubId)).thenReturn(Optional.of(club));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, clubId)).thenReturn(false);
        when(guard.isLead(me, clubId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> newsWorkflowService.createRequest(me, req));
    }

    // ========== updatePendingRequest ==========

    @Test
    void updatePendingRequest_unauthorized_throwsSecurityException() {
        // Arrange
        Long me = 99L;
        Long reqId = 5L;

        Club c = new Club();
        c.setId(10L);

        User creatorUser = new User();
        creatorUser.setId(1L);

        RequestNews r = new RequestNews();
        r.setId(reqId);
        r.setClub(c);
        r.setCreatedBy(creatorUser);
        r.setStatus(RequestStatus.PENDING_CLUB);

        when(requestRepo.findDetailById(reqId)).thenReturn(Optional.of(r));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, c.getId())).thenReturn(false);

        UpdateNewsRequest body = new UpdateNewsRequest();
        body.setTitle("New title");

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> newsWorkflowService.updatePendingRequest(me, reqId, body));
    }

    // ========== staffApproveAndPublish ==========

    @Test
    void staffApproveAndPublish_createNewNewsWhenNoneAttached() throws AppException {
        // Arrange
        Long staffId = 7L;
        Long reqId = 50L;

        User staff = new User();
        staff.setId(staffId);
        staff.setFullName("Staff");

        User creatorUser = new User();
        creatorUser.setId(1L);
        creatorUser.setFullName("Creator");

        Club c = new Club();
        c.setId(10L);
        c.setClubName("CLB Dev");

        RequestNews r = new RequestNews();
        r.setId(reqId);
        r.setStatus(RequestStatus.PENDING_UNIVERSITY);
        r.setRequestTitle("Req title");
        r.setDescription("Req desc");
        r.setThumbnailUrl("thumb.png");
        r.setNewsType("GENERAL");
        r.setCreatedBy(creatorUser);
        r.setClub(c);
        r.setNews(null);

        when(guard.isStaff(staffId)).thenReturn(true);
        when(requestRepo.findById(reqId)).thenReturn(Optional.of(r));

        // newsRepo.save → set id
        when(newsRepo.save(any(News.class))).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setId(200L);
            return n;
        });

        News usedNews = new News();
        usedNews.setId(200L);
        usedNews.setTitle("Final title");
        when(newsMapper.toDto(any(News.class))).thenReturn(new NewsData());

        RequestNews detail = new RequestNews();
        detail.setId(reqId);
        detail.setStatus(RequestStatus.APPROVED_UNIVERSITY);
        when(requestRepo.findDetailById(reqId)).thenReturn(Optional.of(detail));

        NewsRequestResponse dto = NewsRequestResponse.builder()
                .id(reqId)
                .status(RequestStatus.APPROVED_UNIVERSITY)
                .build();
        when(mapper.toDto(detail)).thenReturn(dto);

        when(notificationService.getClubManagers(c.getId())).thenReturn(List.of(99L));

        // Act
        NewsRequestResponse resp =
                newsWorkflowService.staffApproveAndPublish(staffId, reqId, new ApproveNewsRequest());

        // Assert
        assertNotNull(resp);
        assertEquals(reqId, resp.getId());
        assertEquals(RequestStatus.APPROVED_UNIVERSITY, resp.getStatus());

        // broadcast tin đã publish
        verify(webSocketService, times(1)).broadcastSystemWide(
                eq("NEWS"),
                eq("PUBLISHED"),
                any()
        );

        // noti cho creator
        verify(notificationService, times(1)).sendToUser(
                eq(creatorUser.getId()),
                eq(staffId),
                anyString(),
                anyString(),
                eq(NotificationType.NEWS_APPROVED),
                eq(NotificationPriority.HIGH),
                anyString(),
                eq(c.getId()),
                anyLong(),
                any(), // teamId có thể null
                eq(reqId),
                isNull()
        );

        // noti cho managers
        verify(notificationService, times(1)).sendToUsers(
                eq(List.of(99L)),
                eq(staffId),
                anyString(),
                anyString(),
                eq(NotificationType.NEWS_PUBLISHED),
                eq(NotificationPriority.NORMAL),
                anyString(),
                eq(c.getId()),
                anyLong(),
                any(),
                eq(reqId)
        );
    }

    @Test
    void staffApproveAndPublish_wrongStatus_throwsIllegalState() throws AppException {
        // Arrange
        Long staffId = 7L;
        Long reqId = 50L;

        RequestNews r = new RequestNews();
        r.setId(reqId);
        r.setStatus(RequestStatus.PENDING_CLUB);

        when(guard.isStaff(staffId)).thenReturn(true);
        when(requestRepo.findById(reqId)).thenReturn(Optional.of(r));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> newsWorkflowService.staffApproveAndPublish(staffId, reqId, new ApproveNewsRequest()));
    }

    // ========== staffReject ==========

    @Test
    void staffReject_happyPath() throws AppException {
        // Arrange
        Long staffId = 5L;
        Long reqId = 9L;

        Club c = new Club();
        c.setId(10L);

        User creatorUser = new User();
        creatorUser.setId(1L);

        RequestNews r = new RequestNews();
        r.setId(reqId);
        r.setClub(c);
        r.setCreatedBy(creatorUser);
        r.setStatus(RequestStatus.PENDING_UNIVERSITY);

        when(guard.isStaff(staffId)).thenReturn(true);
        when(requestRepo.findById(reqId)).thenReturn(Optional.of(r));

        RequestNews detail = new RequestNews();
        detail.setId(reqId);
        detail.setStatus(RequestStatus.REJECTED_UNIVERSITY);
        when(requestRepo.findDetailById(reqId)).thenReturn(Optional.of(detail));

        NewsRequestResponse dto = NewsRequestResponse.builder()
                .id(reqId)
                .status(RequestStatus.REJECTED_UNIVERSITY)
                .build();
        when(mapper.toDto(detail)).thenReturn(dto);

        // Act
        NewsRequestResponse resp =
                newsWorkflowService.staffReject(staffId, reqId, new RejectNewsRequest());

        // Assert
        assertNotNull(resp);
        assertEquals(RequestStatus.REJECTED_UNIVERSITY, resp.getStatus());

        verify(webSocketService).broadcastToUser(
                eq(creatorUser.getId()),
                eq("NEWS_REQUEST"),
                eq("REJECTED"),
                any(Map.class)
        );
        verify(webSocketService).broadcastToSystemRole(
                eq("STAFF"),
                eq("NEWS_REQUEST"),
                eq("REJECTED"),
                any(Map.class)
        );
    }

    // ========== cancelRequest ==========

    @Test
    void cancelRequest_notAllowed_throwsSecurityException() {
        // Arrange
        Long me = 9L;
        Long reqId = 3L;

        Club c = new Club();
        c.setId(10L);

        User creatorUser = new User();
        creatorUser.setId(1L);

        RequestNews r = new RequestNews();
        r.setId(reqId);
        r.setClub(c);
        r.setCreatedBy(creatorUser);
        r.setStatus(RequestStatus.PENDING_CLUB);

        when(requestRepo.findDetailById(reqId)).thenReturn(Optional.of(r));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, c.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> newsWorkflowService.cancelRequest(me, reqId));
    }

    // ========== helper MockitoAnswer (để tránh warning generic) ==========
    @Test
    void createRequest_managerCreates_success() throws AppException {
        Long me = clubManager.getId();

        CreateNewsRequest dto = new CreateNewsRequest();
        dto.setTitle("Title");
        dto.setContent("Content");
        dto.setClubId(club.getId());

        when(userRepo.findById(me)).thenReturn(Optional.of(clubManager));
        when(clubRepo.findById(club.getId())).thenReturn(Optional.of(club));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, club.getId())).thenReturn(true);

        when(newsRepo.save(any())).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setId(300L);
            return n;
        });

        when(requestRepo.save(any())).thenAnswer(inv -> {
            RequestNews r = inv.getArgument(0);
            r.setId(400L);
            return r;
        });

        // FIXED — không dùng constructor sai
        RequestNews detail = new RequestNews();
        detail.setId(400L);
        detail.setStatus(RequestStatus.PENDING_UNIVERSITY);

        when(requestRepo.findDetailById(400L)).thenReturn(Optional.of(detail));

        // FIXED — dùng builder
        NewsRequestResponse dtoResp = NewsRequestResponse.builder()
                .id(400L)
                .status(RequestStatus.PENDING_UNIVERSITY)
                .build();

        when(mapper.toDto(detail)).thenReturn(dtoResp);

        NewsRequestResponse resp = newsWorkflowService.createRequest(me, dto);

        assertEquals(400L, resp.getId());
    }

    @Test
    void updatePendingRequest_creatorCanEditPendingClub_success() {
        Long me = creator.getId();

        RequestNews r = new RequestNews();
        r.setId(5L);
        r.setStatus(RequestStatus.PENDING_CLUB);
        r.setCreatedBy(creator);
        r.setClub(club);

        when(requestRepo.findDetailById(5L)).thenReturn(Optional.of(r));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, club.getId())).thenReturn(false);

        UpdateNewsRequest body = new UpdateNewsRequest();
        body.setTitle("Updated");

        newsWorkflowService.updatePendingRequest(me, 5L, body);

        assertEquals("Updated", r.getRequestTitle());
    }

    @Test
    void clubApproveAndSubmit_success() throws AppException {
        Long managerId = clubManager.getId();

        RequestNews r = new RequestNews();
        r.setId(100L);
        r.setStatus(RequestStatus.PENDING_CLUB);
        r.setCreatedBy(creator);
        r.setClub(club);
        r.setRequestTitle("Req");

        when(requestRepo.findById(100L)).thenReturn(Optional.of(r));
        when(guard.canApproveAtClub(managerId, club.getId())).thenReturn(true);

        when(userRepo.findBySystemRole_RoleNameIgnoreCase("STAFF"))
                .thenReturn(List.of(creator));

        when(requestRepo.findDetailById(100L)).thenReturn(Optional.of(r));

        // FIXED — dùng builder
        NewsRequestResponse dtoResp = NewsRequestResponse.builder()
                .id(100L)
                .status(RequestStatus.PENDING_UNIVERSITY)
                .build();

        when(mapper.toDto(r)).thenReturn(dtoResp);

        NewsRequestResponse resp =
                newsWorkflowService.clubApproveAndSubmit(managerId, 100L, new ApproveNewsRequest());

        assertEquals(RequestStatus.PENDING_UNIVERSITY, resp.getStatus());
    }

    @Test
    void clubPresidentReject_notPresident_throwsSecurity() {
        RequestNews r = new RequestNews();
        r.setId(10L);
        r.setStatus(RequestStatus.PENDING_CLUB);
        r.setClub(club);
        r.setCreatedBy(creator);

        when(requestRepo.findById(10L)).thenReturn(Optional.of(r));
        when(guard.canRejectAtClub(99L, club.getId())).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> newsWorkflowService.clubPresidentReject(99L, 10L, new RejectNewsRequest()));
    }
    @Test
    void staffDirectPublish_notStaff_throwsSecurity() {
        Long me = 10L;
        when(guard.isStaff(me)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> newsWorkflowService.staffDirectPublish(me, new ApproveNewsRequest()));
    }
    @Test
    void cancelRequest_creatorCanCancelPendingClub_success() {
        Long me = creator.getId();

        RequestNews r = new RequestNews();
        r.setId(10L);
        r.setStatus(RequestStatus.PENDING_CLUB);
        r.setCreatedBy(creator);
        r.setClub(club);

        when(requestRepo.findDetailById(10L)).thenReturn(Optional.of(r));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, club.getId())).thenReturn(false);

        newsWorkflowService.cancelRequest(me, 10L);

        assertEquals(RequestStatus.CANCELED, r.getStatus());
    }


    private static class MockitoAnswer<T> implements org.mockito.stubbing.Answer<T> {
        private final org.mockito.stubbing.Answer<T> delegate;
        MockitoAnswer(org.mockito.stubbing.Answer<T> delegate) {
            this.delegate = delegate;
        }
        @Override
        public T answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
            return delegate.answer(invocation);
        }
    }
    @Test
    void createRequest_leadCorrectTeam_success() throws AppException {
        Long me = creator.getId();
        Long teamId = 88L;

        Team team = new Team();
        team.setId(teamId);

        CreateNewsRequest req = new CreateNewsRequest();
        req.setTitle("T");
        req.setContent("N");
        req.setClubId(club.getId());
        req.setTeamId(teamId);

        when(userRepo.findById(me)).thenReturn(Optional.of(creator));
        when(clubRepo.findById(club.getId())).thenReturn(Optional.of(club));
        when(guard.isStaff(me)).thenReturn(false);
        when(guard.canApproveAtClub(me, club.getId())).thenReturn(false);
        when(guard.isLead(me, club.getId())).thenReturn(true);
        when(guard.isTeamLead(me, club.getId(), teamId)).thenReturn(true);
        when(teamRepo.findById(teamId)).thenReturn(Optional.of(team));

        when(requestRepo.save(any())).thenAnswer(inv -> {
            RequestNews r = inv.getArgument(0);
            r.setId(888L);
            return r;
        });

        RequestNews detail = new RequestNews();
        detail.setId(888L);
        detail.setStatus(RequestStatus.PENDING_CLUB);

        when(requestRepo.findDetailById(888L)).thenReturn(Optional.of(detail));
        when(mapper.toDto(detail)).thenReturn(
                NewsRequestResponse.builder().id(888L).status(RequestStatus.PENDING_CLUB).build()
        );

        NewsRequestResponse resp = newsWorkflowService.createRequest(me, req);

        assertEquals(RequestStatus.PENDING_CLUB, resp.getStatus());

        verify(webSocketService).broadcastToClub(
                eq(club.getId()), eq("NEWS_REQUEST"), eq("CREATED"), any()
        );
    }
    @Test
    void createRequest_emptyTitle_throwsIllegalArgument() {
        CreateNewsRequest req = new CreateNewsRequest();
        req.setTitle("");
        req.setContent("abc");
        req.setClubId(club.getId());

        assertThrows(IllegalArgumentException.class,
                () -> newsWorkflowService.createRequest(1L, req));
    }
    @Test
    void updatePendingRequest_staffCanEditPendingUniversity_success() {
        Long me = staffUser.getId();

        RequestNews r = new RequestNews();
        r.setId(100L);
        r.setStatus(RequestStatus.PENDING_UNIVERSITY);
        r.setClub(club);

        when(requestRepo.findDetailById(100L)).thenReturn(Optional.of(r));
        when(guard.isStaff(me)).thenReturn(true);

        UpdateNewsRequest body = new UpdateNewsRequest();
        body.setTitle("Updated");

        newsWorkflowService.updatePendingRequest(me, 100L, body);

        assertEquals("Updated", r.getRequestTitle());
    }
    @Test
    void clubApproveAndSubmit_wrongStatus_throwsIllegal() {
        Long managerId = clubManager.getId();

        RequestNews r = new RequestNews();
        r.setId(5L);
        r.setStatus(RequestStatus.PENDING_UNIVERSITY); // wrong state
        r.setClub(club);

        when(requestRepo.findById(5L)).thenReturn(Optional.of(r));
        when(guard.canApproveAtClub(managerId, club.getId())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> newsWorkflowService.clubApproveAndSubmit(managerId, 5L, new ApproveNewsRequest()));
    }
    @Test
    void clubPresidentReject_success() throws AppException {
        Long me = clubManager.getId();

        RequestNews r = new RequestNews();
        r.setId(5L);
        r.setStatus(RequestStatus.PENDING_CLUB);
        r.setCreatedBy(creator);
        r.setClub(club);

        when(requestRepo.findById(5L)).thenReturn(Optional.of(r));
        when(guard.canRejectAtClub(me, club.getId())).thenReturn(true);

        NewsRequestResponse dto = NewsRequestResponse.builder()
                .id(5L)
                .status(RequestStatus.REJECTED_CLUB)
                .build();

        when(requestRepo.findDetailById(5L)).thenReturn(Optional.of(r));
        when(mapper.toDto(any())).thenReturn(dto);

        NewsRequestResponse resp = newsWorkflowService.clubPresidentReject(me, 5L, new RejectNewsRequest());

        assertEquals(RequestStatus.REJECTED_CLUB, resp.getStatus());
        verify(webSocketService, times(1)).broadcastToUser(anyLong(), anyString(), anyString(), any());
        verify(webSocketService, times(1)).broadcastToClub(anyLong(), anyString(), anyString(), any());
    }
    @Test
    void staffReject_wrongStatus_throwsIllegal() {
        Long staffId = 10L;

        RequestNews r = new RequestNews();
        r.setId(11L);
        r.setStatus(RequestStatus.PENDING_CLUB);

        when(guard.isStaff(staffId)).thenReturn(true);
        when(requestRepo.findById(11L)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateException.class,
                () -> newsWorkflowService.staffReject(staffId, 11L, new RejectNewsRequest()));
    }
    @Test
    void staffDirectPublish_success() {
        Long staffId = 10L;

        ApproveNewsRequest body = new ApproveNewsRequest();
        body.setTitle("A");
        body.setContent("B");

        User staff = new User();
        staff.setId(staffId);

        when(guard.isStaff(staffId)).thenReturn(true);
        when(userRepo.findById(staffId)).thenReturn(Optional.of(staff));

        when(newsRepo.save(any())).thenAnswer(inv -> {
            News n = inv.getArgument(0);
            n.setId(999L);
            return n;
        });

        when(newsMapper.toDto(any())).thenReturn(new NewsData());

        var result = newsWorkflowService.staffDirectPublish(staffId, body);

        assertEquals(999L, result.getNewsId());
    }
    @Test
    void cancelRequest_staffCancelPendingUniversity_success() {
        Long staffId = 10L;

        RequestNews r = new RequestNews();
        r.setId(5L);
        r.setStatus(RequestStatus.PENDING_UNIVERSITY);
        r.setClub(club);

        when(requestRepo.findDetailById(5L)).thenReturn(Optional.of(r));
        when(guard.isStaff(staffId)).thenReturn(true);

        newsWorkflowService.cancelRequest(staffId, 5L);

        assertEquals(RequestStatus.CANCELED, r.getStatus());
        verify(requestRepo).save(r);
    }










}
