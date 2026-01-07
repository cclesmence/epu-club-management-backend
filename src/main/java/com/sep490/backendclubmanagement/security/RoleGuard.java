package com.sep490.backendclubmanagement.security;

import com.sep490.backendclubmanagement.entity.News;
import com.sep490.backendclubmanagement.entity.Team;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import com.sep490.backendclubmanagement.repository.TeamRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleGuard {

    private final RoleMemberShipRepository rmRepo;
    private final UserRepository userRepo;
    private final SemesterRepository semesterRepo;
    private final TeamRepository teamRepo; // ✅ THÊM

    private Long currentSemesterIdOrNull() {
        return semesterRepo.findCurrentSemester().map(s -> s.getId()).orElse(null);
    }

    // ===== QUYỀN CƠ BẢN =====
    public boolean isStaff(Long userId) {
        return userRepo.isStaffOrAdmin(userId);
    }

    public boolean isClubPresident(Long userId, Long clubId) {
        return rmRepo.isPresidentSimple(userId, clubId);
    }


    public boolean isClubVice(Long userId, Long clubId) {
        return rmRepo.isClubViceExact(userId, clubId, currentSemesterIdOrNull());
    }

    public boolean isLead(Long userId, Long clubId) {
        return rmRepo.isAnyTeamLeadInClub(userId, clubId, currentSemesterIdOrNull());
    }

    // Chủ nhiệm hoặc Phó chủ nhiệm
    public boolean isClubManager(Long userId, Long clubId) {
        return isClubPresident(userId, clubId) || isClubVice(userId, clubId);
    }

    // Trưởng ban hoặc Chủ nhiệm/Phó
    public boolean isClubLeader(Long userId, Long clubId) {
        return isLead(userId, clubId) || isClubManager(userId, clubId);
    }

    public boolean isTeamLead(Long userId, Long clubId, Long teamId) {
        return rmRepo.isTeamLeadExact(userId, clubId, teamId, currentSemesterIdOrNull());
    }

    /** Approve & submit lên Staff: Chủ nhiệm hoặc Phó chủ nhiệm đều được */
    public boolean canApproveAtClub(Long userId, Long clubId) {
        return isClubManager(userId, clubId);
    }

    /** Reject ở cấp CLB: chỉ Chủ nhiệm */
    public boolean canRejectAtClub(Long userId, Long clubId) {
        return isClubPresident(userId, clubId);
    }

    /** Tạo request: Staff/President/Vice/Lead */
    public boolean canCreateNews(Long userId, Long clubId) {
        return isStaff(userId) || isClubLeader(userId, clubId);
    }

    /** Quản lý nháp của CLB: Chủ nhiệm/Phó/Trưởng ban hoặc Staff */
    public boolean canManageClubDraft(Long userId, Long clubId) {
        return isStaff(userId) || isClubLeader(userId, clubId);
    }

    /** Quyền submit nháp lên cấp trên: Trưởng ban hoặc Chủ nhiệm/Phó */
    public boolean canSubmitDraft(Long userId, Long clubId) {
        return isClubLeader(userId, clubId);
    }

    /** Sửa nháp: người tạo, quản lý CLB, hoặc Staff */
    public boolean canEditDraft(Long userId, News draft) {
        Long clubId = draft.getClub().getId();
        return isStaff(userId)
                || draft.getCreatedBy().getId().equals(userId)
                || isClubManager(userId, clubId);
    }

    public boolean canViewClubRequests(Long userId, Long clubId) {
        return isStaff(userId) || isClubManager(userId, clubId);
    }

    public boolean canViewTeamRequests(Long userId, Long clubId, Long teamId) {
        return isStaff(userId) || isClubManager(userId, clubId) || isTeamLead(userId, clubId, teamId);
    }

    // ✅ để controller dùng lấy danh sách team lead id
    public List<Long> findLeadTeamIdsInClub(Long userId, Long clubId) {
        return rmRepo.findLeadTeamIdsInClub(userId, clubId);
    }

    public Optional<Team> findLeadTeamInClub(Long userId, Long clubId) {
        var ids = rmRepo.findLeadTeamIdsInClub(userId, clubId); // ✅ dùng rmRepo
        if (ids.isEmpty()) return Optional.empty();
        return teamRepo.findById(ids.get(0));
    }
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Không thể xác định người dùng hiện tại (chưa đăng nhập).");
        }

        Object principal = auth.getPrincipal();
        String email;

        // Case A: principal là UserDetails (username của bạn chính là email)
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        }
        // Case B: principal là chuỗi (nhiều cấu hình để email ở đây)
        else if (principal instanceof String s) {
            email = s;
        }
        else {
            throw new AccessDeniedException("Không thể xác định email từ principal.");
        }

        return userRepo.findIdByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy người dùng có email: " + email));
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static String firstNonBlank(String... vals) {
        for (String s : vals) if (s != null && !s.isBlank()) return s;
        return null;
    }
}
