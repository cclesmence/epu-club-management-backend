package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.dto.response.MemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamDTO;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.Team;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.TeamRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyClubService {

    private final ClubMemberShipRepository clubMembershipRepository;
    private final RoleMemberShipRepository roleMembershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;  // ✅ thêm dòng này

    // Giả định vai trò Chủ nhiệm có role_level = 1
    private static final int MANAGER_ROLE_LEVEL = 1;

    /**
     * ✅ Lấy danh sách thành viên của CLB mà người dùng hiện tại đang là thành viên.
     */
    public List<MemberDTO> getMembersOfCurrentUserClub() {
        User currentUser = getCurrentUser();

        // Lấy club mà user này đang là thành viên (không cần phải là manager)
        ClubMemberShip membership = clubMembershipRepository.findAllByUserId(currentUser.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("User is not a member of any club."));

        return clubMembershipRepository.findAllMembersByClubId(membership.getClub().getId());
    }

    /**
     * ✅ Lấy danh sách các team (ban/đội) của tất cả CLB mà người dùng hiện tại tham gia.
     */
    public List<TeamDTO> getTeamsOfCurrentUserClubs() {
        User currentUser = getCurrentUser();

        // Tìm tất cả các CLB mà user là thành viên
        List<ClubMemberShip> memberships = clubMembershipRepository.findAllByUserId(currentUser.getId());
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy danh sách clubId
        List<Long> clubIds = memberships.stream()
                .map(m -> m.getClub().getId())
                .collect(Collectors.toList());

        // Lấy danh sách team (Entity)
        List<Team> teams = teamRepository.findAllByClubIdIn(clubIds);

        // Map sang TeamDTO
        return teams.stream()
                .map(team -> new TeamDTO(
                        team.getId(),
                        team.getTeamName(),
                        team.getDescription(),
                        team.getClub() != null ? team.getClub().getId() : null,
                        team.getClub() != null ? team.getClub().getClubName() : null
                ))
                .collect(Collectors.toList());
    }


    /**
     * ✅ Lấy thông tin user hiện tại từ JWT token (Spring Security Context).
     */
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else {
            throw new IllegalStateException("User principal not found in Security Context.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found in database with email: " + email));
    }
}