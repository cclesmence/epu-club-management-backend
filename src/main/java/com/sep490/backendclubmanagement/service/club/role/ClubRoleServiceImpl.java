package com.sep490.backendclubmanagement.service.club.role;

import com.sep490.backendclubmanagement.dto.request.CreateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.response.ClubRoleResponse;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.mapper.ClubRoleMapper;
import com.sep490.backendclubmanagement.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubRoleServiceImpl implements ClubRoleService {

    private final ClubRoleRepository clubRoleRepository;
    private final ClubRoleMapper clubRoleMapper;

    private final RoleMemberShipRepository roleMemberShipRepo;

    private final ClubRepository clubRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final UserService userService;


    @Override
    public List<ClubRoleResponse> getClubRolesByClubId(Long clubId) {
        List<ClubRole> clubRoles = clubRoleRepository.findByClubId(clubId);
        return clubRoleMapper.toDtos(clubRoles);
    }

    @Override
    public boolean isClubLeaderOrVice(Long userId, Long clubId) {
        return roleMemberShipRepo.existsClubAdmin(userId, clubId);
    }

    @Override
    public boolean isTeamLeader(Long userId, Long teamId) {
        return roleMemberShipRepo.existsTeamLeader(userId, teamId, "_HEAD");
    }

    @Override
    @Transactional
    public ClubRoleResponse createClubRole(Long clubId,
                                           CreateClubRoleRequest request) throws AppException {
        // 1. Lấy user hiện tại
        Long currentUserId = userService.getCurrentUserId();

        // 2. Chỉ Chủ nhiệm / Phó chủ nhiệm mới được tạo (check bằng repository existsClubAdmin)
        if (!isClubLeaderOrVice(currentUserId, clubId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 3. Auto-generate roleCode from roleName if not provided
        String roleCode = request.getRoleCode();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            roleCode = generateRoleCode(request.getRoleName());

            // Ensure uniqueness by appending number if needed
            String baseCode = roleCode;
            int counter = 1;
            while (clubRoleRepository.existsByClubIdAndRoleCodeIgnoreCase(clubId, roleCode)) {
                roleCode = baseCode + counter;
                counter++;
            }
        } else {
            // If roleCode is provided, check for duplicates
            if (clubRoleRepository.existsByClubIdAndRoleCodeIgnoreCase(clubId, roleCode)) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Mã vai trò đã tồn tại");
            }
        }

        // 4. Dùng ClubRoleRepository để CHECK TRÙNG roleName
        if (clubRoleRepository.existsByClubIdAndRoleNameIgnoreCase(clubId, request.getRoleName())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tên vai trò đã tồn tại");
        }

        // 5. Check roleLevel (không cho <= 2 để tránh trùng cấp admin)
        if (request.getRoleLevel() == null || request.getRoleLevel() <= 1) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Cấp vai trò phải lớn hơn 1");
        }

        // 6. Lấy club bằng repository
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Club không tồn tại"));

        // 7. Lấy SystemRole bằng repository (nếu có id)
        SystemRole systemRole = null;
        if (request.getSystemRoleId() != null) {
            systemRole = systemRoleRepository.findById(request.getSystemRoleId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "System role không tồn tại"));
        }

        // 8. Build entity mới với roleCode đã được generate
        ClubRole newRole = ClubRole.builder()
                .roleName(request.getRoleName().trim())
                .roleCode(roleCode.trim())
                .description(request.getDescription())
                .roleLevel(request.getRoleLevel())
                .club(club)
                .systemRole(systemRole)
                .build();

        // 9. Lưu DB
        ClubRole saved = clubRoleRepository.save(newRole);

        // 10. Map sang DTO và trả về
        return clubRoleMapper.toDto(saved);
    }

    /**
     * Generate roleCode from roleName
     * Example: "Trưởng ban Truyền thông" -> "TRUONG_BAN_TRUYEN_THONG"
     */
    private String generateRoleCode(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "ROLE_" + System.currentTimeMillis();
        }

        // Remove Vietnamese accents and convert to uppercase
        String normalized = removeVietnameseAccents(roleName.trim());

        // Replace spaces and special characters with underscore
        String code = normalized
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", ""); // Remove leading/trailing underscores

        // Limit length to 50 characters
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }

        return code;
    }

    /**
     * Remove Vietnamese accents from string
     */
    private String removeVietnameseAccents(String str) {
        if (str == null) return null;

        str = str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        str = str.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        str = str.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        str = str.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        str = str.replaceAll("[ìíịỉĩ]", "i");
        str = str.replaceAll("[ÌÍỊỈĨ]", "I");
        str = str.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        str = str.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        str = str.replaceAll("[ùúụủũôừứựửữ]", "u");
        str = str.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        str = str.replaceAll("[ỳýỵỷỹ]", "y");
        str = str.replaceAll("[ỲÝỴỶỸ]", "Y");
        str = str.replaceAll("đ", "d");
        str = str.replaceAll("Đ", "D");

        return str;
    }

    @Override
    @Transactional
    public ClubRoleResponse updateClubRole(Long clubId,
                                           Long roleId,
                                           UpdateClubRoleRequest request) throws AppException {

        // 1. Lấy user hiện tại
        Long currentUserId = userService.getCurrentUserId();

        // 2. Chỉ Chủ nhiệm / Phó chủ nhiệm mới được sửa
        if (!isClubLeaderOrVice(currentUserId, clubId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 3. Lấy role cần sửa
        ClubRole role = clubRoleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 3.1. Đảm bảo role này thuộc đúng club
        if (role.getClub() == null || !role.getClub().getId().equals(clubId)) {
            // không cho sửa role của club khác
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 4. Check trùng roleCode / roleName trong cùng club, exclude role hiện tại
        if (clubRoleRepository.existsByClubIdAndRoleCodeIgnoreCaseAndIdNot(
                clubId, request.getRoleCode(), roleId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        if (clubRoleRepository.existsByClubIdAndRoleNameIgnoreCaseAndIdNot(
                clubId, request.getRoleName(), roleId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // 5. Check roleLevel (không cho chỉnh xuống <= 2)
        if (request.getRoleLevel() == null || request.getRoleLevel() <= 1) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // 6. Cập nhật dữ liệu
        role.setRoleName(request.getRoleName().trim());
        role.setRoleCode(request.getRoleCode().trim());
        role.setDescription(request.getDescription());
        role.setRoleLevel(request.getRoleLevel());

        // 7. Cập nhật SystemRole nếu có
        if (request.getSystemRoleId() != null) {
            SystemRole systemRole = systemRoleRepository.findById(request.getSystemRoleId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
            role.setSystemRole(systemRole);
        } else {
            role.setSystemRole(null);
        }

        // 8. Lưu lại
        ClubRole saved = clubRoleRepository.save(role);

        // 9. Map sang DTO trả về
        return clubRoleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteClubRole(Long clubId, Long roleId) throws AppException {
        // 1. Lấy user hiện tại
        Long currentUserId = userService.getCurrentUserId();

        // 2. Chỉ Chủ nhiệm / Phó chủ nhiệm mới được xoá
        if (!isClubLeaderOrVice(currentUserId, clubId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 3. Lấy role cần xoá
        ClubRole role = clubRoleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 3.1. Đảm bảo role thuộc đúng club
        if (role.getClub() == null || !role.getClub().getId().equals(clubId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 4. Không cho xoá các role "admin" level <= 2 (phòng xoá nhầm Chủ nhiệm/Phó)
        if (role.getRoleLevel() != null && role.getRoleLevel() <= 1) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // 5. Xoá – do ClubRole có cascade ALL tới RoleMemberShip,
        //    các role_memberships liên quan sẽ bị xoá theo
        clubRoleRepository.delete(role);
    }


}
