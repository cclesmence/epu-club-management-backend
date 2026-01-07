package com.sep490.backendclubmanagement.service.team;

import com.sep490.backendclubmanagement.dto.response.MyTeamRoleResponse;
import com.sep490.backendclubmanagement.dto.response.TeamMemberDTO;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.entity.Team;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import com.sep490.backendclubmanagement.repository.TeamRepository;
import com.sep490.backendclubmanagement.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TeamRoleServiceImpl implements TeamRoleService {

    private final TeamRepository teamRepo;
    private final RoleMemberShipRepository rmRepo;
    private final SemesterRepository semesterRepo;
    private final RoleGuard guard;

    @Override
    @Transactional(readOnly = true)
    public MyTeamRoleResponse getMyRole(Long me, Long clubId, Long teamId) {
        // 1) Team phải thuộc CLB
        Team team = teamRepo.findByIdAndClubId(teamId, clubId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ban trong CLB."));

        // 2) Học kỳ hiện tại (nếu có)
        Long semesterId = semesterRepo.findCurrentSemester().map(Semester::getId).orElse(null);

        // 3) Lấy role theo team (đang có sẵn)
        List<String> teamRoles = rmRepo.findMyRoles(me, teamId, semesterId);

        // 4) Lấy role theo CLB (mới thêm)
        List<String> clubRoles = rmRepo.findMyClubRoleNames(me, clubId, semesterId);

        // 5) Gộp lại thành myRoles (không trùng)
        List<String> myRoles = java.util.stream.Stream.concat(clubRoles.stream(), teamRoles.stream())
                .distinct()
                .toList();

        // 6) Xác định có phải thành viên của chính team này không
        //    -> dùng isMyTeam cho đúng nghĩa "member của team", không lẫn Staff
        boolean isMember = rmRepo.isMyTeam(me, clubId, teamId, semesterId);

        // 7) Danh sách thành viên + count
        List<TeamMemberDTO> membersDto = rmRepo.findMembersByTeamIdAndSemesterId(teamId, semesterId);

        // 8) Build output đúng schema FE đang dùng
        return MyTeamRoleResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .description(team.getDescription())
                .member(isMember)
                .myRoles(myRoles) // <-- giờ đã có cả "Chủ nhiệm", "Phó chủ nhiệm", "Trưởng ban", "Phó ban", ...
                .memberCount(membersDto.size())
                .members(membersDto.stream().map(m ->
                        MyTeamRoleResponse.MemberBrief.builder()
                                .userId(m.getUserId())
                                .fullName(m.getFullName())
                                .avatarUrl(m.getAvatarUrl())
                                .roleName(m.getRoleName())
                                .email(m.getEmail())
                                .studentCode(m.getStudentCode())
                                .build()
                ).toList())
                .build();
    }

}
