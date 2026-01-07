package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.dto.response.MyTeamDetailDTO;
import com.sep490.backendclubmanagement.dto.response.TeamMemberDTO;
import com.sep490.backendclubmanagement.dto.response.VisibleTeamDTO;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.entity.Team;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.ResourceNotFoundException;
import com.sep490.backendclubmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubTeamVisibilityService {

    private final RoleMemberShipRepository roleMembershipRepository;
    private final TeamRepository teamRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final ClubMemberShipRepository clubMemberShipRepository;

    /**
     * ✅ MỞ QUYỀN XEM:
     * - Là thành viên ACTIVE của CLB -> thấy TOÀN BỘ team trong CLB (không chỉ team của mình).
     * - Vẫn trả myRoles (nếu user có vai trò trong team đó).
     */
    public List<VisibleTeamDTO> getVisibleTeams(Long clubId, Long semesterIdNullable) {
        User currentUser = getCurrentUser();
        Long semesterId = resolveSemesterId(semesterIdNullable);

        // Chỉ cần là thành viên ACTIVE của CLB (hoặc admin) là được xem toàn bộ team
        boolean isActiveClubMember = clubMemberShipRepository
                .existsByClubIdAndUserIdAndStatusActive(clubId, currentUser.getId());
        boolean isAdmin = roleMembershipRepository.isClubAdmin(currentUser.getId(), clubId, semesterId);

        if (!isActiveClubMember && !isAdmin) {
            throw new ResourceNotFoundException("Bạn không thuộc CLB này.");
        }

        // 1) TOÀN BỘ team của CLB
        List<Team> teams = teamRepository.findAllByClubId(clubId);

        // 2) Đếm member theo team (distinct) theo semester (nếu cần)
        Map<Long, Long> memberCountMap = roleMembershipRepository.countMembersByTeam(clubId, semesterId)
                .stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));

        // 3) Vai trò của current user trên từng team (nếu có)
        Map<Long, List<String>> myRolesMap = roleMembershipRepository.findMyRolesPerTeam(currentUser.getId(), clubId, semesterId)
                .stream()
                .collect(Collectors.groupingBy(
                        r -> (Long) r[0],
                        Collectors.mapping(r -> (String) r[1], Collectors.toList())
                ));

        // 4) Map ra VisibleTeamDTO
        List<VisibleTeamDTO> result = new ArrayList<>(teams.size());
        for (Team t : teams) {
            result.add(new VisibleTeamDTO(
                    t.getId(),
                    t.getTeamName(),
                    t.getDescription(),
                    memberCountMap.getOrDefault(t.getId(), 0L),
                    myRolesMap.getOrDefault(t.getId(), List.of())
            ));
        }
        return result;
    }

    /**
     * ✅ MỞ QUYỀN XEM:
     * - Là thành viên ACTIVE của CLB -> xem được danh sách THÀNH VIÊN của BẤT KỲ team trong CLB.
     * - member = isMyTeam(...) để FE quyết định có hiển thị "Bài đăng" hay không.
     */
    public MyTeamDetailDTO getTeamDetail(Long clubId, Long teamId, Long semesterIdNullable) {
        User currentUser = getCurrentUser();
        Long semesterId = resolveSemesterId(semesterIdNullable);

        // Team phải thuộc CLB (dùng truy vấn ràng buộc clubId)
        Team team = teamRepository.findByIdAndClubId(teamId, clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Team không thuộc CLB này."));

        // CHỈ CẦN là thành viên ACTIVE trong CLB là được truy cập (kể cả không thuộc team)
        boolean isActiveClubMember = clubMemberShipRepository
                .existsByClubIdAndUserIdAndStatusActive(clubId, currentUser.getId());
        if (!isActiveClubMember) {
            throw new ResourceNotFoundException("Bạn không thuộc CLB này.");
        }

        // memberFlag: user có thuộc chính team này không (FE dùng để ẩn/hiện tab Posts)
        boolean isMember = roleMembershipRepository.isMyTeam(currentUser.getId(), clubId, teamId, semesterId);

        Long memberCount = roleMembershipRepository.countDistinctMembers(teamId, semesterId);
        List<String> myRoles = roleMembershipRepository.findMyRoles(currentUser.getId(), teamId, semesterId);

        // Luôn trả danh sách thành viên team vì đã xác nhận thuộc CLB
        List<TeamMemberDTO> members = roleMembershipRepository
                .findMembersByTeamIdAndSemesterId(teamId, semesterId);

        return new MyTeamDetailDTO(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                /* member */ isMember,
                myRoles,
                memberCount,
                members,
                team.getLinkGroupChat()
        );
    }

    /**
     * Giữ nguyên logic cho PRESIDENT (nếu bạn đang dùng).
     */
    public List<VisibleTeamDTO> getAllTeamsForClubPresident(Long clubId) {
        User currentUser = getCurrentUser();

        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new ResourceNotFoundException("Current semester not found."));
        Long semesterId = currentSemester.getId();

        // Kiểm tra user có phải CLUB_OFFICER của kì hiện tại không
        boolean isPresident = roleMembershipRepository.isClubOfficerInCurrentSemester(
                currentUser.getId(),
                clubId,
                semesterId
        );

        if (!isPresident) {
            throw new ResourceNotFoundException(
                    "User is not CLUB_PRESIDENT of this club in the current semester."
            );
        }

        List<Team> teams = teamRepository.findAllByClubId(clubId);

        List<VisibleTeamDTO> result = new ArrayList<>(teams.size());
        for (Team t : teams) {
            result.add(new VisibleTeamDTO(
                    t.getId(),
                    t.getTeamName(),
                    t.getDescription(),
                    0L,
                    List.of()
            ));
        }
        return result;
    }

    // ---------- helpers ----------

    private Long resolveSemesterId(Long semesterIdNullable) {
        if (semesterIdNullable != null) return semesterIdNullable;
        Semester current = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new ResourceNotFoundException("Current semester not found."));
        return current.getId();
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated.");
        }

        Object principal = auth.getPrincipal();
        String email;

        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof String s) {
            if ("anonymousUser".equalsIgnoreCase(s)) {
                throw new IllegalStateException("Anonymous user.");
            }
            email = s;
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found in database with email: " + email));
    }
}
