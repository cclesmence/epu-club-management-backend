package com.sep490.backendclubmanagement.service.role;

import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    /**
     * Lấy system role của user
     */
    public String getUserSystemRole(Long userId) {
        return roleMemberShipRepository.findSystemRoleByUserId(userId)
                .map(r -> r == null ? "STUDENT" : r.trim().toUpperCase())
                .orElse("STUDENT"); // Default role nếu không tìm thấy
    }

    public String getUserSystemRoleStaff(Long userId) {
        return roleMemberShipRepository.findSystemRoleStaff(userId)
                .map(r -> r == null ? "STUDENT" : r.trim().toUpperCase())
                .orElse("STUDENT"); // Default role nếu không tìm thấy
    }


    /**
     * Kiểm tra user có phải CLUB_OFFICER không (dựa vào system role)
     */
    public boolean isClubPresident(Long userId, Long clubId) {
        // Ưu tiên truy vấn trực tiếp role trong CLB
        String role = roleMemberShipRepository.findSystemRoleByUserIdAndClubId(userId, clubId)
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .orElse("");
        if ("CLUB_OFFICER".equals(role)) return true;
        // fallback: logic tổng hợp (bao quát nhiều alias quyền president)
        return roleMemberShipRepository.isClubAdmin(userId, clubId, null);
    }

        /**
         * Kiểm tra user có phải CLUB_OFFICER không (không phụ thuộc club)
         */
        public boolean isClubPresident(Long userId) {
            // True nếu user là PRESIDENT ở bất kỳ CLB nào
            if (roleMemberShipRepository.existsPresidentSomewhere(userId)) return true;
            String systemRole = getUserSystemRole(userId);
            return "CLUB_OFFICER".equals(systemRole);
        }
    
    /**
     * Kiểm tra user có phải TEAM_OFFICER không (dựa vào system role)
     */
    public boolean isClubOfficer(Long userId) {
        if (roleMemberShipRepository.existsOfficerSomewhere(userId)) return true;
        String systemRole = getUserSystemRole(userId);
        return "TEAM_OFFICER".equals(systemRole);
    }
    public boolean isClubOfficer(Long userId, Long clubId) {
        // Ưu tiên theo CLB cụ thể
        String role = roleMemberShipRepository.findSystemRoleByUserIdAndClubId(userId, clubId)
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .orElse("");
        if ("TEAM_OFFICER".equals(role)) return true;
        return roleMemberShipRepository.isClubOfficer(userId, clubId);
    }
    
    /**
     * Kiểm tra user có phải STAFF không (dựa vào system role)
     */
    public boolean isStaff(Long userId) {
        String systemRole = getUserSystemRoleStaff(userId);
        return "STAFF".equals(systemRole);
    }

    /**
     * Kiểm tra user có phải ADMIN (system role) không
     */
    public boolean isAdmin(Long userId) {
        return userRepository.existsByIdAndSystemRole_RoleNameIgnoreCase(userId, "ADMIN");
    }
    
    /**
     * Kiểm tra user có quyền tạo event không
     */
    public boolean canCreateEvent(Long userId, Long clubId) {
        // Staff không có club membership, cần check từ users.system_role_id
        if (isStaff(userId)) {
            return true;
        }
        // Các role khác check từ club membership - check systemRole trong club cụ thể
        if (clubId != null) {
            String systemRoleInClub = roleMemberShipRepository.findSystemRoleByUserIdAndClubId(userId, clubId)
                    .map(r -> r == null ? "" : r.trim().toUpperCase())
                    .orElse("");
            return hasEventCreationPrivilege(systemRoleInClub);
        }
        // Fallback: nếu không có clubId, check global systemRole (cho backward compatibility)
        String systemRole = getUserSystemRole(userId);
        return hasEventCreationPrivilege(systemRole);
    }
    
    /**
     * Lấy danh sách club mà user là president
     * TODO: Implement logic này dựa vào SystemRole
     */
    public List<Club> getClubsWhereUserIsPresident(Long userId) {
        List<Long> clubIds = roleMemberShipRepository.findPresidentClubIdsByUserId(userId);
        if (clubIds == null || clubIds.isEmpty()) {
            return List.of();
        }
        return clubRepository.findAllById(clubIds);
    }

    public boolean isClubTreasurer(Long userId) {
        if (roleMemberShipRepository.existsTreasurerSomewhere(userId)) return true;
        String systemRole = getUserSystemRole(userId);
        return isTreasurerRole(systemRole);
    }

    public boolean isClubTreasurer(Long userId, Long clubId) {
        String role = roleMemberShipRepository.findSystemRoleByUserIdAndClubId(userId, clubId)
                .map(r -> r == null ? "" : r.trim().toUpperCase())
                .orElse("");
        if (isTreasurerRole(role)) return true;
        return roleMemberShipRepository.isClubTreasurer(userId, clubId);
    }

    private boolean isTreasurerRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "CLUB_TREASURE".equals(normalized)
                || "CLUB_TREASURER".equals(normalized)
                || "TREASURER".equals(normalized);
    }

    private boolean hasEventCreationPrivilege(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "CLUB_OFFICER".equals(normalized)
                || "TEAM_OFFICER".equals(normalized)
                || "CLUB_TREASURE".equals(normalized)
                || "CLUB_TREASURER".equals(normalized)
                || "TREASURER".equals(normalized);
    }
}

