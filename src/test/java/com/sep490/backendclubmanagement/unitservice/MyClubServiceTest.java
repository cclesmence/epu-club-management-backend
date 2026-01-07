package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.MemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamDTO;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.TeamRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.club.club.MyClubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyClubServiceTest {

    @Mock private ClubMemberShipRepository clubMemberShipRepository;
    @Mock private RoleMemberShipRepository roleMemberShipRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MyClubService myClubService;

    /**
     * Static mocking MUST be returned so tests can close it.
     */
    private MockedStatic<SecurityContextHolder> mockCurrentUser(User user) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(user.getEmail());
        when(securityContext.getAuthentication()).thenReturn(authentication);

        MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class);
        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        return mockedStatic;
    }

    @Test
    void getMembersOfCurrentUserClub_happyPath() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user@example.com");
        u.setFullName("User A");

        try (var ignored = mockCurrentUser(u)) {

            Club c = new Club();
            c.setId(100L);
            c.setClubName("CLB Dev");

            ClubMemberShip cms = new ClubMemberShip();
            cms.setId(10L);
            cms.setUser(u);
            cms.setClub(c);

            when(clubMemberShipRepository.findAllByUserId(u.getId()))
                    .thenReturn(List.of(cms));

            MemberDTO m1 = new MemberDTO();
            m1.setUserId(5L);
            m1.setFullName("Member 1");

            when(clubMemberShipRepository.findAllMembersByClubId(c.getId()))
                    .thenReturn(List.of(m1));

            List<MemberDTO> result = myClubService.getMembersOfCurrentUserClub();

            assertEquals(1, result.size());
            assertEquals("Member 1", result.get(0).getFullName());
        }
    }

    @Test
    void getMembersOfCurrentUserClub_notMember_throws() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user@example.com");
        u.setFullName("User A");

        try (var ignored = mockCurrentUser(u)) {

            when(clubMemberShipRepository.findAllByUserId(u.getId()))
                    .thenReturn(Collections.emptyList());

            assertThrows(AccessDeniedException.class,
                    () -> myClubService.getMembersOfCurrentUserClub());
        }
    }

    @Test
    void getTeamsOfCurrentUserClubs_happyPath() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user@example.com");
        u.setFullName("User A");

        try (var ignored = mockCurrentUser(u)) {

            Club c = new Club();
            c.setId(100L);
            c.setClubName("CLB Dev");

            ClubMemberShip cms = new ClubMemberShip();
            cms.setId(10L);
            cms.setUser(u);
            cms.setClub(c);

            when(clubMemberShipRepository.findAllByUserId(u.getId()))
                    .thenReturn(List.of(cms));

            when(teamRepository.findAllByClubIdIn(List.of(c.getId())))
                    .thenReturn(List.of(
                            new com.sep490.backendclubmanagement.entity.Team() {{
                                setId(200L);
                                setTeamName("Ban Kỹ thuật");
                                setDescription("Dev");
                                setClub(c);
                            }}
                    ));

            List<TeamDTO> result = myClubService.getTeamsOfCurrentUserClubs();

            assertEquals(1, result.size());
            assertEquals("Ban Kỹ thuật", result.get(0).getTeamName());
            assertEquals(c.getId(), result.get(0).getClubId());
        }
    }

    @Test
    void getTeamsOfCurrentUserClubs_noMembership_returnsEmpty() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user@example.com");
        u.setFullName("User A");

        try (var ignored = mockCurrentUser(u)) {

            when(clubMemberShipRepository.findAllByUserId(u.getId()))
                    .thenReturn(Collections.emptyList());

            List<TeamDTO> result = myClubService.getTeamsOfCurrentUserClubs();
            assertTrue(result.isEmpty());
        }
    }
}
