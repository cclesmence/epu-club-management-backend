package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.exception.ResourceNotFoundException;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.club.club.ClubManagementService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubManagementServiceTest {

    @Mock private ClubMemberShipRepository clubMembershipRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RoleMemberShipRepository roleMembershipRepository;
    @Mock private PostRepository postRepository;
    @Mock private NewsRepository newsRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ClubManagementService service;

    private MockedStatic<SecurityContextHolder> securityContextMock;

    @BeforeEach
    void setupSecurityContext() {
        securityContextMock = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        securityContextMock.close();
    }

    private User mockCurrentUser(Long userId, String email) {
        User u = new User();
        u.setId(userId);
        u.setEmail(email);
        u.setFullName("User " + userId);

        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(email);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(ud);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(authentication);

        securityContextMock.when(SecurityContextHolder::getContext).thenReturn(ctx);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        return u;
    }


    @Test
    void getMyClubs_happyPath() {
        User user = mockCurrentUser(10L, "user@example.com");

        Semester sem = new Semester();
        sem.setId(5L);
        sem.setSemesterCode("2024A");
        sem.setIsCurrent(true);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(sem));

        MyClubDTO dto = new MyClubDTO();
        dto.setClubId(1L);
        dto.setClubName("CLB Dev");

        when(clubMembershipRepository.findClubsByUserIdAndSemesterId(user.getId(), sem.getId()))
                .thenReturn(List.of(dto));

        when(roleMembershipRepository.findClubRolesByUserAndClub(user.getId(), 1L, sem.getId()))
                .thenReturn(List.of("CLUB_PRESIDENT"));

        List<MyClubDTO> result = service.getMyClubs();

        assertEquals(1, result.size());
        assertEquals("CLB Dev", result.get(0).getClubName());
        assertEquals(1, result.get(0).getClubRoles().size());
    }


    @Test
    void getClubManagementDetail_happyPath() {
        User user = mockCurrentUser(10L, "user@example.com");

        Semester sem = new Semester();
        sem.setId(5L);
        sem.setSemesterCode("2024A");
        sem.setIsCurrent(true);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(sem));

        MyClubDTO clubDto = new MyClubDTO();
        clubDto.setClubId(1L);
        clubDto.setClubName("CLB Dev");

        when(clubMembershipRepository.findClubsByUserIdAndSemesterId(user.getId(), sem.getId()))
                .thenReturn(List.of(clubDto));

        Club club = new Club();
        club.setId(1L);
        club.setClubName("CLB Dev");
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));

        Team t = new Team();
        t.setId(100L);
        t.setTeamName("Ban Truyền thông");
        when(teamRepository.findAllByClubId(1L)).thenReturn(List.of(t));

        TeamMemberDTO member = new TeamMemberDTO();
        member.setUserId(20L);
        member.setFullName("Member 1");
        when(roleMembershipRepository.findMembersByTeamIdAndSemesterId(100L, sem.getId()))
                .thenReturn(List.of(member));

        ActivityDTO act1 = new ActivityDTO();
        act1.setId(1000L);
        act1.setType("POST");
        act1.setCreatedAt(LocalDateTime.now());
        when(postRepository.findActivitiesByAuthorIds(anyList())).thenReturn(List.of(act1));
        when(newsRepository.findActivitiesByAuthorIds(anyList())).thenReturn(Collections.emptyList());

        ClubDetailDTO result = service.getClubManagementDetail(1L);

        assertEquals(1L, result.getClubId());
        assertEquals(1, result.getTeams().size());
        assertEquals(1, result.getTeams().get(0).getMembers().size());
        assertEquals(1, result.getTeams().get(0).getActivities().size());
    }


    @Test
    void getClubManagementDetail_notMember_throws() {
        mockCurrentUser(10L, "user@example.com");

        Semester sem = new Semester();
        sem.setId(5L);
        sem.setIsCurrent(true);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(sem));

        when(clubMembershipRepository.findClubsByUserIdAndSemesterId(anyLong(), eq(sem.getId())))
                .thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getClubManagementDetail(1L));
    }


    @Test
    void getUserClubRoles_happyPath() {
        Long userId = 10L;

        Semester sem = new Semester();
        sem.setId(5L);
        sem.setSemesterCode("2024A");
        sem.setIsCurrent(true);
        sem.setStartDate(LocalDate.now().minusDays(1));
        sem.setEndDate(LocalDate.now().plusDays(1));
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(sem));

        MyClubDTO clubDto = new MyClubDTO();
        clubDto.setClubId(1L);
        clubDto.setClubName("CLB Dev");

        when(clubMembershipRepository.findClubsByUserIdAndSemesterId(userId, sem.getId()))
                .thenReturn(List.of(clubDto));

        // 🔥==> THÊM PHẦN NÀY
        User u = new User();
        u.setId(userId);
        u.setFullName("User 10");
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        // <==🔥

        ClubMemberShip cms = new ClubMemberShip();
        cms.setId(100L);
        Club club = new Club();
        club.setId(1L);
        club.setClubName("CLB Dev");
        cms.setClub(club);
        cms.setStatus(ClubMemberShipStatus.ACTIVE);

        when(clubMembershipRepository.findByClubIdAndUserId(1L, userId))
                .thenReturn(cms);

        RoleMemberShip rm = new RoleMemberShip();
        rm.setId(200L);
        ClubRole cr = new ClubRole();
        cr.setId(300L);
        cr.setRoleName("Chủ nhiệm");

        SystemRole sr = new SystemRole();
        sr.setRoleName("CLUB_PRESIDENT");

        cr.setSystemRole(sr);
        rm.setClubRole(cr);

        when(roleMembershipRepository.findByClubMemberShipIdAndSemesterIdAndIsActiveWithFetch(
                cms.getId(), sem.getId(), true))
                .thenReturn(List.of(rm));

        List<ClubRoleInfo> roles = service.getUserClubRoles(userId);

        assertEquals(1, roles.size());
        assertEquals("CLB Dev", roles.get(0).getClubName());
        assertEquals("Chủ nhiệm", roles.get(0).getClubRole());
        assertEquals("CLUB_PRESIDENT", roles.get(0).getSystemRole());
    }

}
