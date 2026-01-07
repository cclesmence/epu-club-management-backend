package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateTeamRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateTeamRequest;
import com.sep490.backendclubmanagement.dto.response.AvailableMemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.DuplicateResourceException;
import com.sep490.backendclubmanagement.mapper.TeamMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.team.TeamServiceImpl;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TeamServiceImplTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMapper teamMapper;
    @Mock private ClubRepository clubRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private ClubRoleRepository clubRoleRepository;
    @Mock private ClubMemberShipRepository clubMemberShipRepository;
    @Mock private RoleMemberShipRepository roleMemberShipRepository;
    @Mock private RoleGuard guard;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private WebSocketService webSocketService;

    @InjectMocks
    private TeamServiceImpl teamService;

    private Club club;
    private Semester semester;
    private Team team;

    @BeforeEach
    void init() {
        club = new Club();
        club.setId(1L);
        club.setClubName("CLB Dev");

        semester = new Semester();
        semester.setId(10L);
        semester.setSemesterCode("2024A");
        semester.setIsCurrent(true);
        semester.setStartDate(LocalDate.now());
        semester.setEndDate(LocalDate.now().plusMonths(3));

        team = new Team();
        team.setId(100L);
        team.setTeamName("Kỹ thuật");
        team.setClub(club);
    }

    // ---------------------------------------
    // GET TEAMS
    // ---------------------------------------
    @Test
    void getTeamsByClubId_success() {
        when(teamRepository.findVisibleTeams(1L)).thenReturn(List.of(team));

        TeamResponse dto = new TeamResponse();
        dto.setId(100L);
        dto.setTeamName("Kỹ thuật");
        when(teamMapper.toDto(team)).thenReturn(dto);

        List<TeamResponse> result = teamService.getTeamsByClubId(1L);

        assertEquals(1, result.size());
        verify(teamRepository).findVisibleTeams(1L);
    }

    // ---------------------------------------
    // CREATE TEAM
    // ---------------------------------------
    @Test
    void createTeam_happyPath() {

        CreateTeamRequest req = new CreateTeamRequest();
        req.setClubId(1L);
        req.setTeamName("Kỹ thuật");
        req.setLeaderUserId(10L);
        req.setViceLeaderUserId(11L);
        req.setMemberUserIds(List.of(12L));

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(semester));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));

        when(guard.getCurrentUserId()).thenReturn(999L);
        when(guard.isClubPresident(999L, 1L)).thenReturn(true);

        when(teamRepository.existsByClubIdAndTeamNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(false);

        // memberships
        ClubMemberShip m1 = buildMembership(10L);
        ClubMemberShip m2 = buildMembership(11L);
        ClubMemberShip m3 = buildMembership(12L);

        when(clubMemberShipRepository.findByUserIdInAndClubId(anyList(), eq(1L)))
                .thenReturn(List.of(m1, m2, m3));

        // deactivate role
        doNothing().when(roleMemberShipRepository)
                .deactivateActiveRolesForUsers(anyList(), eq(1L), eq(semester.getId()));

        // find roles
        when(clubRoleRepository.findByClubIdAndRoleCode(eq(1L), eq("CLUB_TEAM_HEAD")))
                .thenReturn(Optional.of(buildRole("CLUB_TEAM_HEAD")));
        when(clubRoleRepository.findByClubIdAndRoleCode(eq(1L), eq("CLUB_TEAM_DEPUTY")))
                .thenReturn(Optional.of(buildRole("CLUB_TEAM_DEPUTY")));
        when(clubRoleRepository.findByClubIdAndRoleCode(eq(1L), eq("CLUB_MEMBER")))
                .thenReturn(Optional.of(buildRole("CLUB_MEMBER")));

        // team saved
        Team saved = new Team();
        saved.setId(200L);
        saved.setTeamName("Kỹ thuật");
        saved.setClub(club);
        when(teamRepository.save(any())).thenReturn(saved);

        TeamResponse res = new TeamResponse();
        res.setId(200L);
        res.setTeamName("Kỹ thuật");
        when(teamMapper.toDto(saved)).thenReturn(res);

        TeamResponse out = teamService.createTeam(req);

        assertEquals("Kỹ thuật", out.getTeamName());
        verify(webSocketService).broadcastToClub(eq(1L), eq("TEAM"), eq("CREATED"), anyMap());
    }

    @Test
    void createTeam_rejects_duplicateLeaderVice() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setClubId(1L);
        req.setTeamName("KT");
        req.setLeaderUserId(10L);
        req.setViceLeaderUserId(10L);

        assertThrows(DuplicateResourceException.class,
                () -> teamService.createTeam(req));
    }

    @Test
    void createTeam_rejects_noPermission() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setClubId(1L);
        req.setTeamName("ABC");

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(semester));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));

        when(guard.getCurrentUserId()).thenReturn(2L);
        when(guard.isClubPresident(2L, 1L)).thenReturn(false);
        when(guard.isClubVice(2L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> teamService.createTeam(req));
    }

    @Test
    void createTeam_rejects_duplicateName() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setClubId(1L);
        req.setTeamName("Media");

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(semester));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));

        when(guard.getCurrentUserId()).thenReturn(1L);
        when(guard.isClubPresident(1L, 1L)).thenReturn(true);

        when(teamRepository.existsByClubIdAndTeamNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(true);

        assertThrows(AppException.class,
                () -> teamService.createTeam(req));
    }

    @Test
    void createTeam_rejects_invalidName() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setTeamName("ab"); // too short
        req.setClubId(1L);

        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(req));
    }

    // ---------------------------------------
    // UPDATE TEAM
    // ---------------------------------------

    @Test
    void updateTeam_happyPath() {
        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setTeamName("Ban mới");
        req.setDescription("Desc");
        req.setLinkGroupChat("link");

        team.setTeamName("Ban cũ");

        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(999L);
        when(guard.isClubPresident(999L, 1L)).thenReturn(true);

        when(teamRepository.existsByClubIdAndTeamNameIgnoreCaseAndIdNot(eq(1L),
                eq("Ban mới"), eq(100L))).thenReturn(false);

        // active members
        RoleMemberShip rm = new RoleMemberShip();
        ClubMemberShip cms = buildMembership(10L);
        rm.setClubMemberShip(cms);
        rm.setIsActive(true);

        when(roleMemberShipRepository.findByTeamIdAndIsActiveTrue(100L))
                .thenReturn(List.of(rm));

        Team saved = new Team();
        saved.setId(100L);
        saved.setTeamName("Ban mới");
        saved.setClub(club);
        when(teamRepository.save(any())).thenReturn(saved);

        TeamResponse dto = new TeamResponse();
        dto.setId(100L);
        dto.setTeamName("Ban mới");
        when(teamMapper.toDto(saved)).thenReturn(dto);

        TeamResponse result = teamService.updateTeam(100L, req);

        assertEquals("Ban mới", result.getTeamName());
        verify(webSocketService).broadcastToClub(eq(1L), eq("TEAM"), eq("UPDATED"), anyMap());
    }

    @Test
    void updateTeam_reject_noPermission() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(5L);
        when(guard.isClubPresident(5L, 1L)).thenReturn(false);
        when(guard.isClubVice(5L, 1L)).thenReturn(false);

        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setTeamName("ABC");

        assertThrows(AccessDeniedException.class,
                () -> teamService.updateTeam(100L, req));
    }

    @Test
    void updateTeam_reject_duplicateName() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(1L);
        when(guard.isClubPresident(1L, 1L)).thenReturn(true);

        UpdateTeamRequest req = new UpdateTeamRequest();
        req.setTeamName("Trùng tên");

        when(teamRepository.existsByClubIdAndTeamNameIgnoreCaseAndIdNot(eq(1L),
                anyString(), eq(100L)))
                .thenReturn(true);

        assertThrows(AppException.class,
                () -> teamService.updateTeam(100L, req));
    }

    // ---------------------------------------
    // DELETE TEAM
    // ---------------------------------------

    @Test
    void deleteTeam_happyPath_noHistory() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(999L);
        when(guard.isClubPresident(999L, 1L)).thenReturn(true);

        when(roleMemberShipRepository.existsByTeamId(100L)).thenReturn(false);

        teamService.deleteTeam(100L);

        verify(teamRepository).delete(team);
        verify(webSocketService).broadcastToClub(eq(1L), eq("TEAM"), eq("DELETED"), anyMap());
    }

    @Test
    void deleteTeam_reject_historyExists() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(999L);
        when(guard.isClubPresident(999L, 1L)).thenReturn(true);

        when(roleMemberShipRepository.existsByTeamId(100L))
                .thenReturn(true);

        assertThrows(AppException.class,
                () -> teamService.deleteTeam(100L));
    }

    @Test
    void deleteTeam_reject_noPermission() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(guard.getCurrentUserId()).thenReturn(5L);
        when(guard.isClubPresident(5L, 1L)).thenReturn(false);
        when(guard.isClubVice(5L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> teamService.deleteTeam(100L));
    }

    // ---------------------------------------
    // GET AVAILABLE MEMBERS
    // ---------------------------------------

    @Test
    void getAvailableMembers_happyPath() {

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(semester));
        when(clubMemberShipRepository.findAllActiveNonLeadersMemberIds(1L))
                .thenReturn(List.of(10L, 11L));

        User u1 = new User();
        u1.setId(10L);
        u1.setFullName("User 1");
        u1.setEmail("a@b");
        u1.setAvatarUrl("img1");

        User u2 = new User();
        u2.setId(11L);
        u2.setFullName("User 2");
        u2.setEmail("c@d");
        u2.setAvatarUrl("img2");

        when(userRepository.findByIdIn(List.of(10L, 11L)))
                .thenReturn(List.of(u1, u2));

        List<AvailableMemberDTO> result = teamService.getAvailableMembers(1L);

        assertEquals(2, result.size());
    }

    @Test
    void getAvailableMembers_empty() {
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(semester));
        when(clubMemberShipRepository.findAllActiveNonLeadersMemberIds(1L))
                .thenReturn(Collections.emptyList());

        List<AvailableMemberDTO> out = teamService.getAvailableMembers(1L);

        assertTrue(out.isEmpty());
        verify(userRepository, never()).findByIdIn(anyList());
    }

    // ---------------------------------------
    // HELPERS
    // ---------------------------------------

    private ClubMemberShip buildMembership(Long uid) {
        User u = new User();
        u.setId(uid);
        u.setFullName("User " + uid);

        ClubMemberShip cm = new ClubMemberShip();
        cm.setId(uid + 1000);
        cm.setUser(u);
        cm.setClub(club);
        cm.setStatus(ClubMemberShipStatus.ACTIVE);

        return cm;
    }

    private ClubRole buildRole(String code) {
        ClubRole r = new ClubRole();
        r.setId(new Random().nextLong());
        r.setRoleCode(code);
        r.setRoleName(code);
        return r;
    }
}
