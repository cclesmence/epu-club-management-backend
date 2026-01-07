package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.MyTeamDetailDTO;
import com.sep490.backendclubmanagement.dto.response.TeamMemberDTO;
import com.sep490.backendclubmanagement.dto.response.VisibleTeamDTO;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.entity.Team;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.ResourceNotFoundException;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.club.club.ClubTeamVisibilityService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ClubTeamVisibilityServiceTest {

    @Mock private RoleMemberShipRepository roleRepo;
    @Mock private TeamRepository teamRepo;
    @Mock private SemesterRepository semesterRepo;
    @Mock private UserRepository userRepo;
    @Mock private ClubMemberShipRepository clubRepo;

    @InjectMocks
    private ClubTeamVisibilityService service;

    // =======================================================================
    // Utilities
    // =======================================================================
    private void authWithUserDetails(String email) {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(email);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(ud);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private User mockUser(String email, long id) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);

        when(userRepo.findByEmail(email)).thenReturn(Optional.of(u));
        return u;
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    // =======================================================================
    // getVisibleTeams
    // =======================================================================

    @Test
    void getVisibleTeams_happyPath() {
        authWithUserDetails("user@a.com");
        mockUser("user@a.com", 10L);

        Semester sem = new Semester();
        sem.setId(5L);
        when(semesterRepo.findCurrentSemester()).thenReturn(Optional.of(sem));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(1L, 10L))
                .thenReturn(true);

        when(roleRepo.isClubAdmin(10L, 1L, 5L)).thenReturn(false);

        Team t = new Team();
        t.setId(100L);
        t.setTeamName("Team A");

        when(teamRepo.findAllByClubId(1L)).thenReturn(List.of(t));

        // FIXED: MUST RETURN List<Object[]>
        List<Object[]> countList = new ArrayList<>();
        countList.add(new Object[]{100L, 3L});
        when(roleRepo.countMembersByTeam(1L, 5L)).thenReturn(countList);

        List<Object[]> roles = new ArrayList<>();
        roles.add(new Object[]{100L, "LEADER"});
        when(roleRepo.findMyRolesPerTeam(10L, 1L, 5L)).thenReturn(roles);

        List<VisibleTeamDTO> out = service.getVisibleTeams(1L, null);

        assertEquals(1, out.size());
        assertEquals(3L, out.get(0).getMemberCount());
        assertEquals("LEADER", out.get(0).getMyRoles().get(0));
    }

    @Test
    void getVisibleTeams_adminAllowed() {
        authWithUserDetails("admin@mail.com");
        mockUser("admin@mail.com", 10L);

        Semester sem = new Semester();
        sem.setId(9L);
        when(semesterRepo.findCurrentSemester()).thenReturn(Optional.of(sem));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(1L, 10L))
                .thenReturn(false);

        when(roleRepo.isClubAdmin(10L, 1L, 9L)).thenReturn(true);

        when(teamRepo.findAllByClubId(1L)).thenReturn(List.of());

        when(roleRepo.countMembersByTeam(1L, 9L)).thenReturn(new ArrayList<>());
        when(roleRepo.findMyRolesPerTeam(10L, 1L, 9L)).thenReturn(new ArrayList<>());

        List<VisibleTeamDTO> out = service.getVisibleTeams(1L, null);
        assertTrue(out.isEmpty());
    }

    @Test
    void getVisibleTeams_notMemberNotAdmin_throw() {
        authWithUserDetails("x@x.com");
        mockUser("x@x.com", 99L);

        Semester sem = new Semester();
        sem.setId(2L);
        when(semesterRepo.findCurrentSemester()).thenReturn(Optional.of(sem));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(1L, 99L))
                .thenReturn(false);

        when(roleRepo.isClubAdmin(99L, 1L, 2L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVisibleTeams(1L, null));
    }

    // =======================================================================
    // getTeamDetail
    // =======================================================================

    @Test
    void getTeamDetail_happyPath() {
        authWithUserDetails("u@a.com");
        mockUser("u@a.com", 10L);

        Semester sem = new Semester();
        sem.setId(7L);
        when(semesterRepo.findCurrentSemester()).thenReturn(Optional.of(sem));

        Team t = new Team();
        t.setId(100L);
        t.setTeamName("T");
        when(teamRepo.findByIdAndClubId(100L, 1L)).thenReturn(Optional.of(t));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(1L, 10L))
                .thenReturn(true);

        when(roleRepo.isMyTeam(10L, 1L, 100L, 7L)).thenReturn(true);
        when(roleRepo.countDistinctMembers(100L, 7L)).thenReturn(5L);
        when(roleRepo.findMyRoles(10L, 100L, 7L)).thenReturn(List.of("LEADER"));

        TeamMemberDTO member = new TeamMemberDTO();
        member.setUserId(2L);
        member.setFullName("ABC");

        when(roleRepo.findMembersByTeamIdAndSemesterId(100L, 7L))
                .thenReturn(List.of(member));

        MyTeamDetailDTO dto = service.getTeamDetail(1L, 100L, null);

        assertEquals(5L, dto.getMemberCount());
        assertTrue(dto.isMember());
    }

    @Test
    void getTeamDetail_teamNotBelongToClub_throw() {
        authWithUserDetails("z@z.com");
        mockUser("z@z.com", 10L);

        when(teamRepo.findByIdAndClubId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getTeamDetail(1L, 100L, null));
    }

    @Test
    void getTeamDetail_userNotActiveInClub_throw() {
        authWithUserDetails("zz@zz.com");
        mockUser("zz@zz.com", 10L);

        Team t = new Team();
        t.setId(100L);
        when(teamRepo.findByIdAndClubId(100L, 1L))
                .thenReturn(Optional.of(t));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(1L, 10L))
                .thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getTeamDetail(1L, 100L, null));
    }

    // =======================================================================
    // Authentication tests
    // =======================================================================

    @Test
    void getCurrentUser_authNull_throw() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class,
                () -> service.getVisibleTeams(1L, null));
    }

    @Test
    void getCurrentUser_notAuthenticated_throw() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        assertThrows(IllegalStateException.class,
                () -> service.getVisibleTeams(1L, null));
    }

    @Test
    void getCurrentUser_principalString_valid() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@mail.com");

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        mockUser("user@mail.com", 10L);

        Semester sem = new Semester();
        sem.setId(9L);
        when(semesterRepo.findCurrentSemester()).thenReturn(Optional.of(sem));

        when(clubRepo.existsByClubIdAndUserIdAndStatusActive(anyLong(), eq(10L)))
                .thenReturn(true);

        when(teamRepo.findAllByClubId(anyLong())).thenReturn(List.of());
        when(roleRepo.findMyRolesPerTeam(anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getVisibleTeams(1L, null));
    }
}
