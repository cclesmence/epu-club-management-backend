package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.dto.request.CreateClubRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubInfoRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRequest;
import com.sep490.backendclubmanagement.dto.response.ClubDetailData;
import com.sep490.backendclubmanagement.dto.response.ClubDto;
import com.sep490.backendclubmanagement.dto.response.ClubManagementResponse;
import com.sep490.backendclubmanagement.dto.response.ClubPresidentData;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.*;
import com.sep490.backendclubmanagement.service.EpuApiService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.shared.ModelMapperUtils;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.ClubMapper;
import com.sep490.backendclubmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubService implements ClubServiceInterface {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;
    private final CampusRepository campusRepository;
    private final ClubCategoryRepository clubCategoryRepository;
    private final EpuApiService epuApiService;
    private final UserRepository userRepository;
    private final ClubRoleRepository clubRoleRepository;
    private final SemesterRepository semesterRepository;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final RoleService roleService;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    // Maximum allowed upload size for logo/banner: 10 MB
    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024L * 1024L;

    @Override
    @Transactional(readOnly = true)
    public ClubDetailData getClubDetail(Long clubId) throws AppException {
        Club club = clubRepository.findByIdWithDetails(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));
        
        ClubDetailData result = clubMapper.toClubDetailData(club);

        // Set statistics using count queries (avoid N+1 and Cartesian product)
        result.setTotalMembers(clubRepository.countMembersByClubId(club.getId()));
        result.setTotalEvents(clubRepository.countEventsByClubId(club.getId()));
        result.setTotalNews(clubRepository.countNewsByClubId(club.getId()));
        result.setIsRecruiting(clubRepository.hasActiveRecruitment(club.getId()));
        
        // Find all presidents manually and set to result
        List<ClubPresidentData> presidents = findClubPresidentsManually(club);
        result.setPresidents(presidents);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ClubDetailData getClubDetailByCode(String clubCode) throws AppException {
        Club club = clubRepository.findByClubCodeWithDetails(clubCode)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));
        
        ClubDetailData result = clubMapper.toClubDetailData(club);
        
        // Set statistics using count queries (avoid N+1 and Cartesian product)
        result.setTotalMembers(clubRepository.countMembersByClubId(club.getId()));
        result.setTotalEvents(clubRepository.countEventsByClubId(club.getId()));
        result.setTotalNews(clubRepository.countNewsByClubId(club.getId()));
        result.setIsRecruiting(clubRepository.hasActiveRecruitment(club.getId()));
        
        // Find all presidents manually and set to result
        List<ClubPresidentData> presidents = findClubPresidentsManually(club);
        result.setPresidents(presidents);

        return result;
    }
    
    /**
     * Find all club presidents for current semester
     */
    private List<ClubPresidentData> findClubPresidentsManually(Club club) {
        List<ClubPresidentData> presidents = new java.util.ArrayList<>();

        if (club.getClubMemberships() == null) {
            return presidents;
        }

        for (ClubMemberShip membership : club.getClubMemberships()) {
            if (membership.getRoleMemberships() == null) {
                continue;
            }

            for (RoleMemberShip roleMembership : membership.getRoleMemberships()) {
                if (roleMembership.getClubRole() != null 
                    && "CLUB_PRESIDENT".equals(roleMembership.getClubRole().getRoleCode())
                    && Boolean.TRUE.equals(roleMembership.getIsActive())
                    && roleMembership.getSemester() != null
                    && Boolean.TRUE.equals(roleMembership.getSemester().getIsCurrent())) {
                    
                    User user = membership.getUser();
                    if (user != null) {
                        presidents.add(ClubPresidentData.builder()
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .avatarUrl(user.getAvatarUrl())
                                .build());
                    }
                }
            }
        }

        return presidents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubDto> getAllClubs() {
        List<Club> clubs = clubRepository.findAll();
        return ModelMapperUtils.mapList(clubs, ClubDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClubManagementResponse> getClubsByFilter(
            String keyword, Long campusId, Long categoryId, String status,
            Pageable pageable, Long staffId) throws AppException {

        Page<Club> page;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all clubs without keyword filter
            page = clubRepository.getAllClubsByFilter(
                    null,
                    campusId,
                    categoryId,
                    status,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization
            List<Club> filteredList = page.getContent().stream()
                    .filter(club -> {
                        String clubName = normalizeVietnamese(club.getClubName() != null ? club.getClubName() : "");
                        String clubCode = normalizeVietnamese(club.getClubCode() != null ? club.getClubCode() : "");

                        // Split keyword into individual words for better matching
                        String[] keywords = trimmedKeyword.split("\\s+");
                        for (String kw : keywords) {
                            String normalizedKw = normalizeVietnamese(kw);
                            if (clubName.contains(normalizedKw) || clubCode.contains(normalizedKw)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<Club> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            page = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            page = clubRepository.getAllClubsByFilter(
                    keyword,
                    campusId,
                    categoryId,
                    status,
                    pageable
            );
        }

        List<ClubManagementResponse> content = page.getContent().stream()
                .map(club -> {
                    ClubManagementResponse response = clubMapper.toClubManagementResponse(club);
                    response.setTotalMembers(clubRepository.countMembersByClubId(club.getId()));
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.<ClubManagementResponse>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1) // Convert to 1-based pagination
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public ClubManagementResponse createClub(CreateClubRequest request, Long staffId) throws AppException {

        // Validate club code uniqueness
        if (clubRepository.findByClubCode(request.getClubCode()).isPresent()) {
            throw new AppException(ErrorCode.CLUB_CODE_EXISTED);
        }

        // Validate club name uniqueness
        if (clubRepository.findByClubName(request.getClubName()).isPresent()) {
            throw new AppException(ErrorCode.CLUB_NAME_EXISTED);
        }

        // Validate campus
        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));

        // Validate category
        ClubCategory category = clubCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_CATEGORY_NOT_FOUND));

        // Validate president email via EPU API
        var epuProfile = epuApiService.findProfileByEmail(request.getPresidentEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_VALID));

        // Tìm hoặc tạo user từ email
        User president = userRepository.findByEmailIgnoreCase(request.getPresidentEmail())
                .orElseGet(() -> createUserFromEpuProfile(epuProfile));

        // Build club entity
        Club club = Club.builder()
                .clubName(request.getClubName())
                .clubCode(request.getClubCode())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .campus(campus)
                .clubCategory(category)
                .build();

        Club savedClub = clubRepository.save(club);
        log.info("Created new club with ID: {} and code: {}", savedClub.getId(), savedClub.getClubCode());

        // Tạo các role mặc định cho CLB
        createDefaultClubRoles(savedClub);

        // Tạo membership cho chủ CLB
        createPresidentMembership(savedClub, president);

        // Send notification to the new club president (async for better performance)
        try {
            String actionUrl = "/myclub/" + savedClub.getId();
            String title = "Bạn được chỉ định làm Chủ nhiệm CLB";
            String message = "Bạn đã được chỉ định làm Chủ nhiệm của CLB " + savedClub.getClubName() +
                    " (" + savedClub.getClubCode() + "). Chúc mừng bạn!";

            notificationService.sendToUsers(
                    List.of(president.getId()),
                    staffId,
                    title,
                    message,
                    NotificationType.CLUB_ROLE_ASSIGNED,
                    NotificationPriority.HIGH,
                    actionUrl,
                    savedClub.getId(),
                    null,
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to send club creation notification: {}", e.getMessage());
        }

        ClubManagementResponse response = clubMapper.toClubManagementResponse(savedClub);
        response.setTotalMembers(clubRepository.countMembersByClubId(savedClub.getId()));
        response.setTotalEvents(clubRepository.countEventsByClubId(savedClub.getId()));
        response.setTotalPosts(clubRepository.countNewsByClubId(savedClub.getId()));
        return response;
    }

    /**
     * Tạo user mới từ thông tin EPU API
     */
    private User createUserFromEpuProfile(java.util.Map<String, Object> epuProfile) {
        User user = User.builder()
                .email((String) epuProfile.get("email"))
                .fullName((String) epuProfile.get("fullName"))
                .studentCode((String) epuProfile.get("studentCode"))
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Tạo các role mặc định cho câu lạc bộ theo hình ảnh:
     * - CLUB_PRESIDENT (Chủ nhiệm) - level 1
     * - CLUB_VICE_PRESIDENT (Phó Chủ nhiệm) - level 2
     * - CLUB_TEAM_HEAD (Trưởng ban) - level 3
     * - CLUB_TEAM_DEPUTY (Phó ban) - level 4
     * - CLUB_TREASURER (Thủ quỹ) - level 3
     * - CLUB_MEMBER (Thành viên) - level 6
     */
    private void createDefaultClubRoles(Club club) {
        List<ClubRole> defaultRoles = List.of(
                ClubRole.builder()
                        .club(club)
                        .roleName("Chủ nhiệm")
                        .roleCode("CLUB_PRESIDENT")
                        .roleLevel(1)
                        .description("Người đứng đầu câu lạc bộ, quản lý toàn bộ hoạt động.")
                        .systemRole(systemRoleRepository.findByRoleName("CLUB_OFFICER").orElse(null))
                        .build(),
                ClubRole.builder()
                        .club(club)
                        .roleName("Phó chủ nhiệm")
                        .roleCode("CLUB_VICE_PRESIDENT")
                        .roleLevel(2)
                        .description("Phó Chủ nhiệm - trợ giúp Chủ nhiệm.")
                        .systemRole(systemRoleRepository.findByRoleName("CLUB_OFFICER").orElse(null))
                        .build(),
                ClubRole.builder()
                        .club(club)
                        .roleName("Trưởng ban")
                        .roleCode("CLUB_TEAM_HEAD")
                        .roleLevel(3)
                        .description("Trưởng ban - phụ trách 1 ban chuyên môn.")
                        .systemRole(systemRoleRepository.findByRoleName("TEAM_OFFICER").orElse(null))
                        .build(),
                ClubRole.builder()
                        .club(club)
                        .roleName("Phó ban")
                        .roleCode("CLUB_TEAM_DEPUTY")
                        .roleLevel(4)
                        .description("Phó ban - trợ giúp Trưởng ban.")
                        .systemRole(systemRoleRepository.findByRoleName("TEAM_OFFICER").orElse(null))
                        .build(),
                ClubRole.builder()
                        .club(club)
                        .roleName("Thủ quỹ")
                        .roleCode("CLUB_TREASURER")
                        .roleLevel(3)
                        .description("Người quản lý tài chính của CLB.")
                        .systemRole(systemRoleRepository.findByRoleName("CLUB_TREASURER").orElse(null))
                        .build(),
                ClubRole.builder()
                        .club(club)
                        .roleName("Thành viên")
                        .roleCode("CLUB_MEMBER")
                        .roleLevel(6)
                        .description("Thành viên chung của CLB.")
                        .systemRole(systemRoleRepository.findByRoleName("MEMBER").orElse(null))
                        .build()
        );

        clubRoleRepository.saveAll(defaultRoles);
        log.info("Created {} default roles for club ID: {}", defaultRoles.size(), club.getId());
    }

    /**
     * Tạo membership và gán role CLUB_PRESIDENT cho chủ CLB
     */
    private void createPresidentMembership(Club club, User president) throws AppException {
        // Lấy semester hiện tại
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Tạo club membership
        ClubMemberShip membership = ClubMemberShip.builder()
                .user(president)
                .club(club)
                .joinDate(java.time.LocalDate.now())
                .status(ClubMemberShipStatus.ACTIVE)
                .build();

        ClubMemberShip savedMembership = clubMemberShipRepository.save(membership);

        // Lấy role CLUB_PRESIDENT
        ClubRole presidentRole = clubRoleRepository.findByClubIdAndRoleCode(club.getId(), "CLUB_PRESIDENT")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Tạo role membership
        RoleMemberShip roleMembership = RoleMemberShip.builder()
                .clubMemberShip(savedMembership)
                .clubRole(presidentRole)
                .semester(currentSemester)
                .isActive(true)
                .build();

        roleMemberShipRepository.save(roleMembership);
        log.info("Assigned CLUB_PRESIDENT role to user {} in club {}", president.getEmail(), club.getClubName());
    }

    @Override
    @Transactional
    public ClubManagementResponse updateClub(Long clubId, UpdateClubRequest request, Long staffId) throws AppException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        // Update club code if provided and different
        if (request.getClubCode() != null && !request.getClubCode().equals(club.getClubCode())) {
            if (clubRepository.findByClubCode(request.getClubCode()).isPresent()) {
                throw new AppException(ErrorCode.CLUB_CODE_EXISTED);
            }
            club.setClubCode(request.getClubCode());
        }

        // Update club name if provided and different
        if (request.getClubName() != null && !request.getClubName().equals(club.getClubName())) {
            if (clubRepository.findByClubName(request.getClubName()).isPresent()) {
                throw new AppException(ErrorCode.CLUB_NAME_EXISTED);
            }
            club.setClubName(request.getClubName());
        }

        // Update campus if provided
        if (request.getCampusId() != null) {
            Campus campus = campusRepository.findById(request.getCampusId())
                    .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
            club.setCampus(campus);
        }

        // Update category if provided
        if (request.getCategoryId() != null) {
            ClubCategory category = clubCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CLUB_CATEGORY_NOT_FOUND));
            club.setClubCategory(category);
        }

        // Update other fields
        if (request.getDescription() != null) club.setDescription(request.getDescription());
        if (request.getStatus() != null) club.setStatus(request.getStatus());

        Club updatedClub = clubRepository.save(club);
        log.info("Updated club with ID: {}", clubId);

        ClubManagementResponse response = clubMapper.toClubManagementResponse(updatedClub);
        response.setTotalMembers(clubRepository.countMembersByClubId(updatedClub.getId()));
        return response;
    }

    /**
     * Get all active members of a club
     * @param clubId Club ID
     * @return List of user IDs who are active members
     */
    private List<Long> getActiveClubMembers(Long clubId) {
        List<ClubMemberShip> activeMembers = clubMemberShipRepository
                .findByClubIdAndStatus(clubId, ClubMemberShipStatus.ACTIVE);
        return activeMembers.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateClub(Long clubId, Long staffId) throws AppException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        club.setStatus("UNACTIVE");
        clubRepository.save(club);
        log.info("Deactivated club with ID: {}", clubId);

        // Send notification to all club members about deactivation (async)
        try {
            List<Long> memberIds = getActiveClubMembers(clubId);

            if (!memberIds.isEmpty()) {
                String actionUrl = "/myclub/" + clubId;
                String title = "Câu lạc bộ đã bị vô hiệu hóa";
                String message = "CLB " + club.getClubName() + " đã bị vô hiệu hóa bởi nhà trường. " +
                        "Mọi hoạt động của CLB sẽ tạm ngưng cho đến khi được kích hoạt lại.";

                notificationService.sendToUsers(
                        memberIds,
                        staffId,
                        title,
                        message,
                        NotificationType.SYSTEM_WARNING,
                        NotificationPriority.HIGH,
                        actionUrl,
                        clubId,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            log.error("Failed to send club deactivation notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void activateClub(Long clubId, Long staffId) throws AppException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        club.setStatus("ACTIVE");
        clubRepository.save(club);
        log.info("Activated club with ID: {}", clubId);

        // Send notification to all club members about activation (async)
        try {
            List<Long> memberIds = getActiveClubMembers(clubId);

            if (!memberIds.isEmpty()) {
                String actionUrl = "/myclub/" + clubId;
                String title = "Câu lạc bộ đã được kích hoạt lại";
                String message = "CLB " + club.getClubName() + " đã được kích hoạt lại bởi nhà trường. " +
                        "Các hoạt động của CLB có thể tiếp tục.";

                notificationService.sendToUsers(
                        memberIds,
                        staffId,
                        title,
                        message,
                        NotificationType.SYSTEM_ANNOUNCEMENT,
                        NotificationPriority.HIGH,
                        actionUrl,
                        clubId,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            log.error("Failed to send club activation notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClubManagementResponse getClubForManagement(Long clubId, Long staffId) throws AppException {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        ClubManagementResponse response = clubMapper.toClubManagementResponse(club);
        response.setTotalMembers(clubRepository.countMembersByClubId(club.getId()));
        response.setTotalEvents(clubRepository.countEventsByClubId(club.getId()));
        response.setTotalPosts(clubRepository.countNewsByClubId(club.getId()));
        // Find all presidents manually and set to result
        List<ClubPresidentData> presidents = findClubPresidentsManually(club);
        response.setPresidents(presidents);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClubDetailData getClubInfo(Long clubId, Long userId) throws AppException {
        // Kiểm tra user có phải thành viên ACTIVE của club không
        boolean isMember = clubMemberShipRepository.existsByUserIdAndClubIdAndStatus(
            userId, clubId, ClubMemberShipStatus.ACTIVE
        );
        if (!isMember) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Lấy thông tin club
        return getClubDetail(clubId);
    }

    @Override
    @Transactional
    public ClubDetailData updateClubInfo(Long clubId, UpdateClubInfoRequest request, Long userId, MultipartFile logoFile, MultipartFile bannerFile) throws AppException {
        // Tìm club
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        // Check if club is active (only active clubs can update information)
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // ===== Handle logo removal =====
        if (Boolean.TRUE.equals(request.getRemoveLogo())) {
            club.setLogoUrl(null);
            log.info("Removed logo for club {}", clubId);
        }
        // ===== Handle logo file upload =====
        else if (logoFile != null && !logoFile.isEmpty()) {
            // Validate file size
            if (logoFile.getSize() > MAX_UPLOAD_SIZE_BYTES) {
                log.warn("Logo upload rejected for club {}: file size {} exceeds {} bytes", clubId, logoFile.getSize(), MAX_UPLOAD_SIZE_BYTES);
                // Provide custom message indicating 10MB limit
                throw new AppException(ErrorCode.FILE_TOO_LARGE, "Kích thước tập tin vượt quá giới hạn 10MB");
            }

            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadImage(logoFile, "club/logos");
                club.setLogoUrl(uploadResult.url());
                log.info("Uploaded logo for club {}: {}", clubId, uploadResult.url());
            } catch (Exception e) {
                log.error("Failed to upload logo for club {}: {}", clubId, e.getMessage());
                throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
            }
        }

        // ===== Handle banner removal =====
        if (Boolean.TRUE.equals(request.getRemoveBanner())) {
            club.setBannerUrl(null);
            log.info("Removed banner for club {}", clubId);
        }
        // ===== Handle banner file upload =====
        else if (bannerFile != null && !bannerFile.isEmpty()) {
            // Validate file size
            if (bannerFile.getSize() > MAX_UPLOAD_SIZE_BYTES) {
                log.warn("Banner upload rejected for club {}: file size {} exceeds {} bytes", clubId, bannerFile.getSize(), MAX_UPLOAD_SIZE_BYTES);
                // Provide custom message indicating 10MB limit
                throw new AppException(ErrorCode.FILE_TOO_LARGE, "Kích thước tập tin vượt quá giới hạn 10MB");
            }

            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadImage(bannerFile, "club/banners");
                club.setBannerUrl(uploadResult.url());
                log.info("Uploaded banner for club {}: {}", clubId, uploadResult.url());
            } catch (Exception e) {
                log.error("Failed to upload banner for club {}: {}", clubId, e.getMessage());
                throw new RuntimeException("Failed to upload template file: " + e.getMessage(), e);
            }
        }

        // ===== Validate and update clubCode (allow updating clubCode) =====
        if (request.getClubCode() != null) {
            String newCode = request.getClubCode().trim();
            if (!newCode.equals(club.getClubCode())) {
                // Nếu có club khác đã dùng mã này -> lỗi
                clubRepository.findByClubCode(newCode).ifPresent(existing -> {
                    if (!existing.getId().equals(clubId)) {
                        throw new RuntimeException("CLUB_CODE_EXISTED");
                    }
                });
                club.setClubCode(newCode);
            }
        }

        // ===== Validate and update clubName (cannot update to an existing name) =====
        if (request.getClubName() != null) {
            String newName = request.getClubName().trim();
            if (!newName.equals(club.getClubName())) {
                clubRepository.findByClubName(newName).ifPresent(existing -> {
                    if (!existing.getId().equals(clubId)) {
                        throw new RuntimeException("CLUB_NAME_EXISTED");
                    }
                });
                club.setClubName(newName);
            }
        }

        // Cập nhật các field còn lại (chỉ cập nhật khi khác null)
        if (request.getDescription() != null) {
            club.setDescription(request.getDescription());
        }
        // Only update logoUrl from request if no file was uploaded
        if (request.getLogoUrl() != null && (logoFile == null || logoFile.isEmpty())) {
            club.setLogoUrl(request.getLogoUrl());
        }
        // Only update bannerUrl from request if no file was uploaded
        if (request.getBannerUrl() != null && (bannerFile == null || bannerFile.isEmpty())) {
            club.setBannerUrl(request.getBannerUrl());
        }
        if (request.getEmail() != null) {
            club.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            club.setPhone(request.getPhone());
        }
        if (request.getFbUrl() != null) {
            club.setFbUrl(request.getFbUrl());
        }
        if (request.getIgUrl() != null) {
            club.setIgUrl(request.getIgUrl());
        }
        if (request.getTtUrl() != null) {
            club.setTtUrl(request.getTtUrl());
        }
        if (request.getYtUrl() != null) {
            club.setYtUrl(request.getYtUrl());
        }
        if(request.getCategoryId() != 0) {
            ClubCategory category = clubCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CLUB_CATEGORY_NOT_FOUND));
            club.setClubCategory(category);
        }

        // Lưu thay đổi — trước sẽ ném RuntimeException nếu trùng tên/mã, chuyển sang AppException
        try {
            clubRepository.save(club);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if ("CLUB_CODE_EXISTED".equals(msg)) {
                throw new AppException(ErrorCode.CLUB_CODE_EXISTED);
            }
            if ("CLUB_NAME_EXISTED".equals(msg)) {
                throw new AppException(ErrorCode.CLUB_NAME_EXISTED);
            }
            throw ex;
        }

        log.info("Club officer {} updated club {} information", userId, clubId);

        // Trả về thông tin club đã cập nhật
        return getClubDetail(clubId);
    }

    private String normalizeVietnamese(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("đ", "d").replace("Đ", "d");
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }
}

