package com.sep490.backendclubmanagement.service.member;

import com.sep490.backendclubmanagement.dto.response.CurrentTermResponse;
import com.sep490.backendclubmanagement.dto.response.ImportMemberError;
import com.sep490.backendclubmanagement.dto.response.ImportMembersResponse;
import com.sep490.backendclubmanagement.dto.response.MemberHistoryResponse;
import com.sep490.backendclubmanagement.dto.response.MemberResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.SimpleMemberResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.EpuApiService;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService{

    private final ClubMemberShipRepository clubMemberShipRepository;
    private final SemesterRepository semesterRepository;
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final ClubRoleRepository clubRoleRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final EpuApiService epuApiService;


    @Override
    public PageResponse<MemberResponse> getMembersWithFilters(
            Long clubId,
            ClubMemberShipStatus status,
            Long semesterId,
            Long roleId,
            Boolean isActive,
            String searchTerm,
            Pageable pageable) {

        // 🚀 OPTIMIZED: Lấy semesterId hiện tại nếu không được chỉ định
        final Long effectiveSemesterId; // Must be final for lambda
        if (semesterId == null) {
            // Chỉ query 1 lần để lấy current semester
            Semester currentSemester = semesterRepository.findAll().stream()
                    .filter(Semester::getIsCurrent)
                    .findFirst()
                    .orElse(null);
            effectiveSemesterId = currentSemester != null ? currentSemester.getId() : null;
        } else {
            effectiveSemesterId = semesterId;
        }

        // Normalize search term
        final String normalizedSearch = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        // 🚀 OPTIMIZED: Single query với JOIN FETCH để load tất cả relationships
        // Search sẽ được filter trong Java (sau query) để support tìm kiếm không dấu
        org.springframework.data.domain.Page<ClubMemberShip> memberPage =
                clubMemberShipRepository.findMembersWithFiltersOptimized(
                        clubId,
                        status,
                        effectiveSemesterId,
                        roleId,
                        isActive,
                        pageable
                );

        // 🔍 SEARCH FILTER: Filter theo searchTerm trong Java với accent-insensitive
        List<ClubMemberShip> filteredMembers = memberPage.getContent();
        if (normalizedSearch != null) {
            filteredMembers = filteredMembers.stream()
                    .filter(cms -> com.sep490.backendclubmanagement.util.VietnameseTextNormalizer.matchesAny(
                            normalizedSearch,
                            cms.getUser().getFullName(),
                            cms.getUser().getStudentCode()
                    ))
                    .toList();
        }

        // 🚀 OPTIMIZED: Map các entities đã được JOIN FETCH loaded
        List<MemberResponse> memberResponses = filteredMembers.stream()
                .map(cms -> mapToMemberResponse(cms, effectiveSemesterId))
                .toList();

        return PageResponse.<MemberResponse>builder()
                .content(memberResponses)
                .pageNumber(memberPage.getNumber())
                .pageSize(memberPage.getSize())
                .totalElements(memberPage.getTotalElements())
                .totalPages(memberPage.getTotalPages())
                .hasNext(memberPage.hasNext())
                .hasPrevious(memberPage.hasPrevious())
                .build();
    }

    @Override
    public PageResponse<MemberResponse> getLeftMembers(
            Long clubId,
            String searchTerm,
            Pageable pageable) {

        // Normalize search term
        final String normalizedSearch = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        // 🚀 OPTIMIZED: Single query với JOIN FETCH để load tất cả relationships
        org.springframework.data.domain.Page<ClubMemberShip> memberPage =
                clubMemberShipRepository.findLeftMembersOptimized(
                        clubId,
                        pageable
                );

        // 🔍 SEARCH FILTER: Filter theo searchTerm trong Java với accent-insensitive
        List<ClubMemberShip> filteredMembers = memberPage.getContent();
        if (normalizedSearch != null) {
            filteredMembers = filteredMembers.stream()
                    .filter(cms -> com.sep490.backendclubmanagement.util.VietnameseTextNormalizer.matchesAny(
                            normalizedSearch,
                            cms.getUser().getFullName(),
                            cms.getUser().getStudentCode()
                    ))
                    .toList();
        }

        // 🚀 OPTIMIZED: Map các entities đã được JOIN FETCH loaded
        List<MemberResponse> memberResponses = filteredMembers.stream()
                .map(cms -> mapToMemberResponse(cms, null)) // No specific semester for left members
                .toList();

        return PageResponse.<MemberResponse>builder()
                .content(memberResponses)
                .pageNumber(memberPage.getNumber())
                .pageSize(memberPage.getSize())
                .totalElements(memberPage.getTotalElements())
                .totalPages(memberPage.getTotalPages())
                .hasNext(memberPage.hasNext())
                .hasPrevious(memberPage.hasPrevious())
                .build();
    }

    @Override
    public void updateMemberRole(Long clubId, Long userId, Long roleId, Long semesterId, Long currentUserId) throws AppException {
        ClubMemberShip cms = clubMemberShipRepository.findByClubIdAndUserId(clubId, userId);
        if (cms == null || cms.getStatus() == ClubMemberShipStatus.LEFT) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Semester semester = resolveSemester(semesterId);
        ClubRole clubRole = clubRoleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        
        Long curentUserLogin = userService.getCurrentUserId();
        if (curentUserLogin == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!curentUserLogin.equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Integer currentUserRoleLevel = getUserRoleLevel(currentUserId, clubId, semesterId);
        validateRoleAssignmentPermission(currentUserRoleLevel, clubRole.getRoleLevel());


        List<RoleMemberShip> rms = roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(cms.getId(), semester.getId());
        RoleMemberShip target = rms.stream().findFirst().orElse(null);
        
        if (target == null) {
            // Create new record if none exists for this semester
            target = new RoleMemberShip();
            target.setClubMemberShip(cms);
            target.setSemester(semester);
        }
        // Update existing record with new role (no duplicate creation)
        target.setClubRole(clubRole);
        target.setIsActive(true);
        roleMemberShipRepository.save(target);

        // 🔔 Gửi notification cho member được assign role
        try {
            if (!userId.equals(currentUserId)) {
                // Không gửi notification nếu tự assign role cho mình
                Club club = clubRepository.findById(clubId).orElse(null);
                if (club != null) {
                    String title = "Bạn đã được gán vai trò mới trong " + club.getClubName();
                    String message = "Vai trò: " + clubRole.getRoleName();
                    String actionUrl = "/clubs/" + clubId + "/members";

                    notificationService.sendToUser(
                            userId,
                            currentUserId, // actor (người thực hiện assign)
                            title,
                            message,
                            NotificationType.CLUB_ROLE_ASSIGNED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            clubId,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null, // relatedRequestId
                            null  // relatedEventId
                    );

                    log.info("[Member] Notification sent to user {}: role assigned {}", userId, clubRole.getRoleName());
                }
            }
        } catch (Exception e) {
            log.error("[Member] Failed to send role assignment notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break role assignment
        }
    }

    @Override
    public void updateMemberTeam(Long clubId, Long userId, Long teamId, Long semesterId) throws AppException {
        ClubMemberShip cms = clubMemberShipRepository.findByClubIdAndUserId(clubId, userId);
        if (cms == null || cms.getStatus() == ClubMemberShipStatus.LEFT) {
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Semester semester = resolveSemester(semesterId);

        // Find existing role membership for this semester - should be only one per member per semester
        List<RoleMemberShip> rms = roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(cms.getId(), semester.getId());
        RoleMemberShip target = rms.stream().findFirst().orElse(null);
        
        if (target == null) {
            // Create new record if none exists for this semester
            target = new RoleMemberShip();
            target.setClubMemberShip(cms);
            target.setSemester(semester);
        }
        // Update existing record with new team (no duplicate creation)
        Team teamRef = new Team();
        teamRef.setId(teamId);
        target.setTeam(teamRef);
        roleMemberShipRepository.save(target);
    }

    @Override
    public void updateMemberActiveStatus(Long clubId, Long userId, boolean isActive, Long semesterId) {
        ClubMemberShip cms = clubMemberShipRepository.findByClubIdAndUserId(clubId, userId);
        if (cms == null || cms.getStatus() == ClubMemberShipStatus.LEFT) {
            throw new IllegalStateException("Member not found or already left club");
        }
        Semester semester = resolveSemester(semesterId);
        List<RoleMemberShip> rms = roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(cms.getId(), semester.getId());

        if (isActive) {
            // Activate: ensure there is at least ONE role membership record in this semester
            RoleMemberShip target = rms.stream().findFirst().orElse(null);
            if (target == null) {
                target = new RoleMemberShip();
                target.setClubMemberShip(cms);
                target.setSemester(semester);
            }
            target.setIsActive(true);
            roleMemberShipRepository.save(target);
        } else {
            // Pause: per business rule, inactive means NO role membership record in that semester
            for (RoleMemberShip rm : rms) {
                roleMemberShipRepository.delete(rm);
            }
        }
    }

    @Override
    public void removeMemberFromClub(Long clubId, Long userId, String reason) {
        ClubMemberShip cms = clubMemberShipRepository.findByClubIdAndUserId(clubId, userId);
        if (cms == null) {
            throw new IllegalStateException("Member not found");
        }
        cms.setStatus(ClubMemberShipStatus.LEFT);
        cms.setEndDate(java.time.LocalDate.now());
        clubMemberShipRepository.save(cms);

        // 🔔 Gửi notification cho member bị remove
        try {
            Club club = clubRepository.findById(clubId).orElse(null);
            if (club != null) {
                String title = "Bạn đã bị xóa khỏi " + club.getClubName();
                String message = reason != null && !reason.trim().isEmpty()
                        ? "Lý do: " + reason
                        : "Bạn không còn là thành viên của câu lạc bộ này";
                String actionUrl = "/clubs";

                notificationService.sendToUser(
                        userId,
                        null, // actor (system/admin)
                        title,
                        message,
                        NotificationType.CLUB_MEMBER_REMOVED,
                        NotificationPriority.HIGH,
                        actionUrl,
                        clubId,
                        null, // relatedNewsId
                        null, // relatedTeamId
                        null, // relatedRequestId
                        null  // relatedEventId
                );

                log.info("[Member] Notification sent to user {}: removed from club {}", userId, clubId);
            }
        } catch (Exception e) {
            log.error("[Member] Failed to send removal notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break member removal
        }
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        }
        return semesterRepository.findAll().stream()
                .filter(Semester::getIsCurrent)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No current semester configured"));
    }




    private Integer getUserRoleLevel(Long userId, Long clubId, Long semesterId) {
        ClubMemberShip cms = clubMemberShipRepository.findByClubIdAndUserId(clubId, userId);
        if (cms == null) {
            return 999; // No membership = lowest priority
        }
        
        Semester semester = resolveSemester(semesterId);
        List<RoleMemberShip> rms = roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(cms.getId(), semester.getId());
        
        return rms.stream()
                .filter(rm -> rm.getClubRole() != null && Boolean.TRUE.equals(rm.getIsActive()))
                .map(rm -> rm.getClubRole().getRoleLevel())
                .min(Integer::compareTo)
                .orElse(999); // No active role = lowest priority
    }


    private void validateRoleAssignmentPermission(Integer currentUserRoleLevel, Integer targetRoleLevel) throws AppException {
        if (currentUserRoleLevel == null || targetRoleLevel == null) {
            throw new AppException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        if (currentUserRoleLevel >= targetRoleLevel) {
            throw new AppException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
    }


    private MemberResponse mapToMemberResponse(ClubMemberShip clubMemberShip, Long querySemesterId) {
        User user = clubMemberShip.getUser();

        List<Object[]> semesterRows = semesterRepository.findSemestersWithRoleByMembership(
                clubMemberShip.getId(),
                clubMemberShip.getJoinDate(),
                clubMemberShip.getEndDate()
        );

        long totalTerms = semesterRows.stream()
                .map(row -> ((Semester) row[0]).getId())
                .distinct()
                .count();

        CurrentTermResponse currentTermResponse = null;

        // If querySemesterId is provided, show info for that specific semester
        if (querySemesterId != null) {
            currentTermResponse = semesterRows.stream()
                    .map(row -> {
                        Semester sem = (Semester) row[0];
                        RoleMemberShip rm = (RoleMemberShip) row[1];
                        if (!sem.getId().equals(querySemesterId)) {
                            return null;
                        }
                        return CurrentTermResponse.builder()
                                .semesterName(sem.getSemesterName())
                                .semesterCode(sem.getSemesterCode())
                                .roleName(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleName() : null)
                                .roleCode(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleCode() : null)
                                .roleLevel(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleLevel() : null)
                                .teamName(rm != null && rm.getTeam() != null ? rm.getTeam().getTeamName() : null)
                                .attendanceRate(100) // TODO: Tính thật dựa trên attendance
                                .status(clubMemberShip.getStatus().name())
                                .isActive(rm != null && Boolean.TRUE.equals(rm.getIsActive()))
                                .startDate(sem.getStartDate().toString())
                                .endDate(sem.getEndDate().toString())
                                .build();
                    })
                    .filter(r -> r != null)
                    .findFirst()
                    .orElse(null);
        } else {
            // If no specific semester query, show current semester info
            currentTermResponse = semesterRows.stream()
                    .map(row -> {
                        Semester sem = (Semester) row[0];
                        RoleMemberShip rm = (RoleMemberShip) row[1];
                        if (!Boolean.TRUE.equals(sem.getIsCurrent())) {
                            return null;
                        }
                        return CurrentTermResponse.builder()
                                .semesterName(sem.getSemesterName())
                                .semesterCode(sem.getSemesterCode())
                                .roleName(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleName() : null)
                                .roleCode(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleCode() : null)
                                .roleLevel(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleLevel() : null)
                                .teamName(rm != null && rm.getTeam() != null ? rm.getTeam().getTeamName() : null)
                                .attendanceRate(100) // TODO: Tính thật dựa trên attendance
                                .status(clubMemberShip.getStatus().name())
                                .isActive(rm != null && Boolean.TRUE.equals(rm.getIsActive()))
                                .startDate(sem.getStartDate().toString())
                                .endDate(sem.getEndDate().toString())
                                .build();
                    })
                    .filter(r -> r != null)
                    .findFirst()
                    .orElse(null);
        }

        // Fallback: if current term is null or inactive, choose nearest active term from history
        if (currentTermResponse == null || Boolean.FALSE.equals(currentTermResponse.getIsActive())) {
            CurrentTermResponse fallback = null;

            if (querySemesterId != null) {
                // If querying specific semester but no role found, show semester info without role
                fallback = semesterRows.stream()
                        .map(row -> {
                            Semester sem = (Semester) row[0];
                            if (!sem.getId().equals(querySemesterId)) {
                                return null;
                            }
                            return CurrentTermResponse.builder()
                                    .semesterName(sem.getSemesterName())
                                    .semesterCode(sem.getSemesterCode())
                                    .roleName(null)
                                    .roleCode(null)
                                    .roleLevel(null)
                                    .teamName(null)
                                    .attendanceRate(100)
                                    .status(clubMemberShip.getStatus().name())
                                    .isActive(false)
                                    .startDate(sem.getStartDate().toString())
                                    .endDate(sem.getEndDate().toString())
                                    .build();
                        })
                        .filter(r -> r != null)
                        .findFirst()
                        .orElse(null);
            } else {
                // If no specific semester query, find nearest active term from history
                fallback = semesterRows.stream()
                        .map(row -> {
                            Semester sem = (Semester) row[0];
                            RoleMemberShip rm = (RoleMemberShip) row[1];

                            if (rm == null || !Boolean.TRUE.equals(rm.getIsActive())) return null;
                            return CurrentTermResponse.builder()
                                    .semesterName(sem.getSemesterName())
                                    .semesterCode(sem.getSemesterCode())
                                    .roleName(rm.getClubRole() != null ? rm.getClubRole().getRoleName() : null)
                                    .roleCode(rm.getClubRole() != null ? rm.getClubRole().getRoleCode() : null)
                                    .roleLevel(rm.getClubRole() != null ? rm.getClubRole().getRoleLevel() : null)
                                    .teamName(rm.getTeam() != null ? rm.getTeam().getTeamName() : null)
                                    .attendanceRate(100)
                                    .status(clubMemberShip.getStatus().name())
                                    .isActive(false)
                                    .startDate(sem.getStartDate().toString())
                                    .endDate(sem.getEndDate().toString())
                                    .build();
                        })
                        .filter(r -> r != null)
                        .findFirst()
                        .orElse(null);
            }

            if (fallback != null) {
                currentTermResponse = fallback;
            }
        }

        Set<String> seen = new HashSet<>();
        List<MemberHistoryResponse> history = semesterRows.stream()
                .map(row -> {
                    Semester sem = (Semester) row[0];
                    RoleMemberShip rm = (RoleMemberShip) row[1];
                    return MemberHistoryResponse.builder()
                            .semesterName(sem.getSemesterName())
                            .semesterCode(sem.getSemesterCode())
                            .roleName(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleName() : null)
                            .roleCode(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleCode() : null)
                            .roleLevel(rm != null && rm.getClubRole() != null ? rm.getClubRole().getRoleLevel() : null)
                            .teamName(rm != null && rm.getTeam() != null ? rm.getTeam().getTeamName() : null)
                            .status(clubMemberShip.getStatus().name())
                            .isActive(rm != null && Boolean.TRUE.equals(rm.getIsActive()))
                            .startDate(sem.getStartDate().toString())
                            .endDate(sem.getEndDate().toString())
                            .build();
                })
                .filter(h -> {
                    String key = h.getSemesterCode();
                    return seen.add(key);
                })
                .toList();

        return MemberResponse.builder()
                .userId(user.getId())
                .studentCode(user.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .clubName(clubMemberShip.getClub().getClubName())
                .clubCode(clubMemberShip.getClub().getClubCode())
                .membershipStatus(clubMemberShip.getStatus().name())
                .joinDate(clubMemberShip.getJoinDate().toString())
                .endDate(clubMemberShip.getEndDate() != null ? clubMemberShip.getEndDate().toString() : null)
                .totalAttendanceRate(90) // TODO: Tính thật
                .totalTerms((int) totalTerms)
                .lastActive("2025-10-18") // TODO: Lấy từ hoạt động gần nhất
                .currentTerm(currentTermResponse)
                .history(history)
                .build();
    }

    @Override
    public List<SimpleMemberResponse> getAllActiveMembersForSelection(Long clubId) {
        // Get all members that are currently ACTIVE (not LEFT)
        List<ClubMemberShip> activeMembers = clubMemberShipRepository.findByClubIdAndStatus(
                clubId,
                ClubMemberShipStatus.ACTIVE
        );

        // Map to simple response with only basic info needed for selection
        return activeMembers.stream()
                .map(cms -> {
                    User user = cms.getUser();
                    return SimpleMemberResponse.builder()
                            .userId(user.getId())
                            .studentCode(user.getStudentCode())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .avatarUrl(user.getAvatarUrl())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public ImportMembersResponse importMembersFromExcel(Long clubId, MultipartFile file, Long currentUserId) throws Exception {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        int totalRows = 0;
        int processedUsers = 0;
        int processedHistories = 0;
        int createdUsers = 0;
        int updatedUsers = 0;
        int createdMemberships = 0;
        int updatedMemberships = 0;
        int createdRoleMemberships = 0;
        int updatedRoleMemberships = 0;
        List<ImportMemberError> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }

            // Map headers
            Row header = sheet.getRow(0);
            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell c : header) {
                String key = c.getStringCellValue().trim().toLowerCase();
                colIndex.put(key, c.getColumnIndex());
            }

            // Required headers
            String[] required = {"student_code", "full_name", "semester_code"};
            for (String r : required) {
                if (!colIndex.containsKey(r)) {
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }
            }

            // Track processed users
            Set<String> processedUserCodes = new HashSet<>();

            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                totalRows++;

                try {
                    String studentCode = readCell(row, colIndex.get("student_code"));
                    String fullName = readCell(row, colIndex.get("full_name"));
                    String semesterCode = readCell(row, colIndex.get("semester_code"));
                    String email = colIndex.containsKey("email") ? readCell(row, colIndex.get("email")) : null;
                    String phone = colIndex.containsKey("phone") ? readCell(row, colIndex.get("phone")) : null;
                    String roleCode = colIndex.containsKey("role_code") ? readCell(row, colIndex.get("role_code")) : null;
                    String teamName = colIndex.containsKey("team_name") ? readCell(row, colIndex.get("team_name")) : null;
                    String isActiveStr = colIndex.containsKey("is_active") ? readCell(row, colIndex.get("is_active")) : "true";
                    String joinDateStr = colIndex.containsKey("join_date") ? readCell(row, colIndex.get("join_date")) : null;

                    if (studentCode == null || studentCode.isEmpty()) {
                        throw new IllegalArgumentException("student_code is empty");
                    }
                    if (semesterCode == null || semesterCode.isEmpty()) {
                        throw new IllegalArgumentException("semester_code is empty");
                    }

                    // ✅ VALIDATE EMAIL với EPU API
                    if (email != null && !email.isEmpty()) {
                        var profile = epuApiService.findProfileByEmail(email);
                        if (profile.isEmpty()) {
                            throw new IllegalArgumentException("Email " + email + " is not in allowed users list");
                        }
                    }

                    // Find or create user
                    User user = userRepository.findByStudentCode(studentCode).orElse(null);
                    boolean userCreated = false;
                    if (user == null) {
                        user = User.builder()
                                .studentCode(studentCode)
                                .fullName(fullName)
                                .email(email)
                                .phoneNumber(phone)
                                .isActive(true)
                                .build();
                        user = userRepository.save(user);
                        userCreated = true;
                        createdUsers++;
                    } else {
                        // Update user info
                        boolean changed = false;
                        if (fullName != null && !fullName.isEmpty() && !fullName.equals(user.getFullName())) {
                            user.setFullName(fullName);
                            changed = true;
                        }
                        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
                            // ✅ VALIDATE EMAIL trước khi update
                            var profile = epuApiService.findProfileByEmail(email);
                            if (profile.isEmpty()) {
                                throw new IllegalArgumentException("Email " + email + " is not in allowed users list");
                            }
                            user.setEmail(email);
                            changed = true;
                        }
                        if (phone != null && !phone.isEmpty() && !phone.equals(user.getPhoneNumber())) {
                            user.setPhoneNumber(phone);
                            changed = true;
                        }
                        if (changed) {
                            userRepository.save(user);
                            updatedUsers++;
                        }
                    }

                    if (!processedUserCodes.contains(studentCode)) {
                        processedUsers++;
                        processedUserCodes.add(studentCode);
                    }

                    // Find semester
                    Semester semester = semesterRepository.findBySemesterCode(semesterCode)
                            .orElseThrow(() -> new IllegalArgumentException("Semester not found: " + semesterCode));

                    // Find or create ClubMemberShip
                    ClubMemberShip membership = clubMemberShipRepository.findByClubIdAndUserId(clubId, user.getId());
                    boolean membershipCreated = false;
                    if (membership == null) {
                        LocalDate joinDate = joinDateStr != null ? parseDate(joinDateStr) : LocalDate.now();
                        membership = ClubMemberShip.builder()
                                .user(user)
                                .club(club)
                                .joinDate(joinDate)
                                .status(ClubMemberShipStatus.ACTIVE)
                                .build();
                        membership = clubMemberShipRepository.save(membership);
                        membershipCreated = true;
                        createdMemberships++;
                    } else {
                        // Update if needed
                        if (membership.getStatus() != ClubMemberShipStatus.ACTIVE) {
                            membership.setStatus(ClubMemberShipStatus.ACTIVE);
                            membership.setEndDate(null);
                            clubMemberShipRepository.save(membership);
                            updatedMemberships++;
                        }
                    }

                    // Find or create RoleMemberShip
                    Optional<RoleMemberShip> rmOpt = roleMemberShipRepository.findByClubMemberShipAndSemester(membership, semester);
                    RoleMemberShip roleMemberShip = rmOpt.orElse(new RoleMemberShip());
                    boolean rmCreated = !rmOpt.isPresent();

                    roleMemberShip.setClubMemberShip(membership);
                    roleMemberShip.setSemester(semester);

                    // Set role
                    if (roleCode != null && !roleCode.isEmpty()) {
                        ClubRole clubRole = clubRoleRepository.findByClubIdAndRoleCode(clubId, roleCode)
                                .orElse(null);
                        roleMemberShip.setClubRole(clubRole);
                    }

                    // Set team
                    if (teamName != null && !teamName.isEmpty()) {
                        Team team = teamRepository.findByClubIdAndTeamName(clubId, teamName)
                                .orElse(null);
                        roleMemberShip.setTeam(team);
                    }

                    // Set active status
                    boolean isActive = "true".equalsIgnoreCase(isActiveStr) || "1".equals(isActiveStr);
                    roleMemberShip.setIsActive(isActive);

                    roleMemberShipRepository.save(roleMemberShip);
                    if (rmCreated) {
                        createdRoleMemberships++;
                    } else {
                        updatedRoleMemberships++;
                    }

                    processedHistories++;

                } catch (Exception exRow) {
                    errors.add(ImportMemberError.builder()
                            .row(r + 1)
                            .studentCode(readCell(row, colIndex.get("student_code")))
                            .semesterCode(readCell(row, colIndex.get("semester_code")))
                            .message(exRow.getMessage())
                            .build());
                }
            }
        }

        String summary = String.format(
                "Processed %d users (%d created, %d updated), %d memberships (%d created, %d updated), %d role histories (%d created, %d updated)",
                processedUsers, createdUsers, updatedUsers,
                createdMemberships + updatedMemberships, createdMemberships, updatedMemberships,
                createdRoleMemberships + updatedRoleMemberships, createdRoleMemberships, updatedRoleMemberships
        );

        return ImportMembersResponse.builder()
                .totalRows(totalRows)
                .processedUsers(processedUsers)
                .processedHistories(processedHistories)
                .createdUsers(createdUsers)
                .updatedUsers(updatedUsers)
                .createdMemberships(createdMemberships)
                .updatedMemberships(updatedMemberships)
                .createdRoleMemberships(createdRoleMemberships)
                .updatedRoleMemberships(updatedRoleMemberships)
                .errors(errors)
                .summary(summary)
                .build();
    }

    private String readCell(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        if (c.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            double d = c.getNumericCellValue();
            long l = (long) d;
            if (l == d) return String.valueOf(l);
            return String.valueOf(d);
        }
        if (c.getCellType() == CellType.BOOLEAN) return String.valueOf(c.getBooleanCellValue());
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
