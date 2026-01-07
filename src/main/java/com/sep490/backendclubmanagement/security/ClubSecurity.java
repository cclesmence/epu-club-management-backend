package com.sep490.backendclubmanagement.security;

import com.sep490.backendclubmanagement.dto.response.ClubRoleInfo;
import com.sep490.backendclubmanagement.repository.RecruitmentApplicationRepository;
import com.sep490.backendclubmanagement.repository.RecruitmentRepository;
import com.sep490.backendclubmanagement.repository.ReportRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.club.club.ClubManagementService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Security helper component for checking club-based permissions and system roles
 * Uses club roles fetched from database in real-time instead of JWT token
 * and system roles from Spring Security authorities
 */
@Component("clubSecurity")
@Slf4j
@RequiredArgsConstructor
public class ClubSecurity {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ClubManagementService clubManagementService;

    /**
     * Check if current user has a specific club role in the given club
     * @param clubId ID of the club
     * @param expectedClubRole Name of the club role (e.g., "Chủ nhiệm", "Phó chủ nhiệm")
     * @return true if user has the role, false otherwise
     */
    public boolean hasClubRole(Long clubId, String expectedClubRole) {
        try {
            List<ClubRoleInfo> roles = getClubRolesFromDatabase();
            if (roles == null || roles.isEmpty()) {
                log.debug("No club roles found for current user");
                return false;
            }

            boolean hasRole = roles.stream()
                    .anyMatch(r -> clubId.equals(r.getClubId())
                            && expectedClubRole.equalsIgnoreCase(r.getClubRole()));

            log.debug("User has role '{}' in club {}: {}", expectedClubRole, clubId, hasRole);
            return hasRole;
        } catch (Exception e) {
            log.error("Error checking club role", e);
            return false;
        }
    }

    /**
     * Check if current user has a specific system role within a club
     * @param clubId ID of the club
     * @param expectedSystemRole System role (e.g., "CLUB_OFFICER", "CLUB_MEMBER")
     * @return true if user has the system role in the club, false otherwise
     */
    public boolean hasSystemRoleInClub(Long clubId, String expectedSystemRole) {
        try {
            List<ClubRoleInfo> roles = getClubRolesFromDatabase();
            if (roles == null || roles.isEmpty()) {
                log.debug("No club roles found for current user");
                return false;
            }

            boolean hasRole = roles.stream()
                    .anyMatch(r -> clubId.equals(r.getClubId())
                            && expectedSystemRole.equalsIgnoreCase(r.getSystemRole()));

            log.debug("User has system role '{}' in club {}: {}", expectedSystemRole, clubId, hasRole);
            return hasRole;
        } catch (Exception e) {
            log.error("Error checking system role in club", e);
            return false;
        }
    }

    /**
     * Check if current user is an officer (has CLUB_OFFICER system role) in the given club
     * @param clubId ID of the club
     * @return true if user is an officer, false otherwise
     */
    public boolean isClubOfficerInClub(Long clubId) {
        return hasSystemRoleInClub(clubId, "CLUB_OFFICER");
    }

    /**
     * Check if current user is an officer (has TEAM_OFFICER system role) in the given club
     * @param clubId ID of the club
     * @return true if user is an officer, false otherwise
     */
    public boolean isTeamOfficerInClub(Long clubId) {
        return hasSystemRoleInClub(clubId, "TEAM_OFFICER");
    }

    /**
     * Check if current user is an officer (has TREASURER system role) in the given club
     * @param clubId ID of the club
     * @return true if user is an officer, false otherwise
     */
    public boolean isTreasureInClub(Long clubId) {
        return hasSystemRoleInClub(clubId, "TREASURER");
    }

    /**
     * Check if current user is an officer (has TEAM_OFFICER or CLUB_OFFICER system role) in the given club
     * @param clubId ID of the club
     * @return true if user is an officer, false otherwise
     */
    public boolean isTeamOfficerOrClubOfficerOrTreasurerInClub(Long clubId) {
        return isClubOfficerInClub(clubId) || isTeamOfficerInClub(clubId) || isTreasureInClub(clubId);
    }

    /**
     * Check if current user is CLUB_OFFICER or CLUB_TREASURER in the given club
     * Used for finance-related operations
     * @param clubId ID of the club
     * @return true if user is CLUB_OFFICER or CLUB_TREASURER, false otherwise
     */
    public boolean isClubOfficerOrTreasureInClub(Long clubId) {
        return isClubOfficerInClub(clubId) || isTreasureInClub(clubId);
    }

    /**
     * Check if current user is TEAM_OFFICER, CLUB_TREASURER, or CLUB_OFFICER in the given club
     * CLUB_TREASURER has all permissions of TEAM_OFFICER
     * @param clubId ID of the club
     * @return true if user has any of these roles, false otherwise
     */
    public boolean isTeamOfficerOrTreasureOrClubOfficerInClub(Long clubId) {
        return isClubOfficerInClub(clubId) || isTreasureInClub(clubId) || isTeamOfficerInClub(clubId);
    }

    /**
     * Check if current user is a member (has any role) in the given club
     * @param clubId ID of the club
     * @return true if user has any role in the club, false otherwise
     */
    public boolean isMemberOfClub(Long clubId) {
        try {
            List<ClubRoleInfo> roles = getClubRolesFromDatabase();
            if (roles == null || roles.isEmpty()) {
                log.debug("No club roles found for current user");
                return false;
            }

            boolean isMember = roles.stream()
                    .anyMatch(r -> clubId.equals(r.getClubId()));

            log.debug("User is member of club {}: {}", clubId, isMember);
            return isMember;
        } catch (Exception e) {
            log.error("Error checking club membership", e);
            return false;
        }
    }

    /**
     * Check if current user has any of the specified club roles in the given club
     * @param clubId ID of the club
     * @param clubRoles List of acceptable club role names
     * @return true if user has any of the roles, false otherwise
     */
    public boolean hasAnyClubRole(Long clubId, String... clubRoles) {
        try {
            List<ClubRoleInfo> roles = getClubRolesFromDatabase();
            if (roles == null || roles.isEmpty()) {
                return false;
            }

            for (String expectedRole : clubRoles) {
                if (roles.stream().anyMatch(r -> clubId.equals(r.getClubId())
                        && expectedRole.equalsIgnoreCase(r.getClubRole()))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking any club role", e);
            return false;
        }
    }

    // ============= SYSTEM ROLE CHECKS (User's global system role, not club-specific) =============

    /**
     * Get current user's system role from database
     * @return System role name or null if not available
     */
    private String getSystemRoleFromDatabase() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                log.debug("Cannot get user ID, returning null system role");
                return null;
            }

            // Query system role from database
            String systemRole = userRepository.findSystemRoleNameByUserId(userId).orElse(null);
            log.debug("Fetched system role from database for user ID {}: {}", userId, systemRole);

            return systemRole;
        } catch (Exception e) {
            log.error("Error fetching system role from database", e);
            return null;
        }
    }

    /**
     * Check if current user has a specific system role (e.g., ADMIN, STAFF, STUDENT)
     * This checks the user's global system role from database in real-time
     * @param expectedSystemRole System role name (e.g., "ADMIN", "STAFF", "STUDENT")
     * @return true if user has the system role, false otherwise
     */
    public boolean hasSystemRole(String expectedSystemRole) {
        try {
            String systemRole = getSystemRoleFromDatabase();
            if (systemRole == null) {
                log.debug("No system role found for current user");
                return false;
            }

            boolean hasRole = expectedSystemRole.equalsIgnoreCase(systemRole);
            log.debug("User has system role '{}': {}", expectedSystemRole, hasRole);
            return hasRole;
        } catch (Exception e) {
            log.error("Error checking system role", e);
            return false;
        }
    }

    /**
     * Check if current user is ADMIN
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return hasSystemRole("ADMIN");
    }

    /**
     * Check if current user is STAFF
     * @return true if user is staff, false otherwise
     */
    public boolean isStaff() {
        return hasSystemRole("STAFF");
    }

    /**
     * Check if current user is STUDENT
     * @return true if user is student, false otherwise
     */
    public boolean isStudent() {
        return hasSystemRole("STUDENT");
    }

    /**
     * Check if current user is ADMIN or STAFF
     * @return true if user is admin or staff, false otherwise
     */
    public boolean isAdminOrStaff() {
        return isAdmin() || isStaff();
    }

    /**
     * Check if current user has any of the specified system roles
     * @param systemRoles List of system role names
     * @return true if user has any of the roles, false otherwise
     */
    public boolean hasAnySystemRole(String... systemRoles) {
        for (String role : systemRoles) {
            if (hasSystemRole(role)) {
                return true;
            }
        }
        return false;
    }

    // ============= COMBINED CHECKS =============

    /**
     * Check if user is ADMIN OR has a specific club role
     * @param clubId ID of the club
     * @param clubRole Club role name
     * @return true if user is admin or has the club role, false otherwise
     */
    public boolean isAdminOrHasClubRole(Long clubId, String clubRole) {
        return isAdmin() || hasClubRole(clubId, clubRole);
    }

    /**
     * Check if user is ADMIN OR STAFF OR has a specific club role
     * @param clubId ID of the club
     * @param clubRole Club role name
     * @return true if user is admin/staff or has the club role, false otherwise
     */
    public boolean isAdminOrStaffOrHasClubRole(Long clubId, String clubRole) {
        return isAdminOrStaff() || hasClubRole(clubId, clubRole);
    }

    /**
     * Fetch club roles from database in real-time for current user
     * @return List of ClubRoleInfo or empty list if not available
     */
    private List<ClubRoleInfo> getClubRolesFromDatabase() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                log.debug("Cannot get user ID, returning empty roles");
                return Collections.emptyList();
            }

            // Fetch club roles from database using ClubManagementService
            List<ClubRoleInfo> roles = clubManagementService.getUserClubRoles(userId);
            log.debug("Fetched {} club roles from database for user ID: {}",
                roles != null ? roles.size() : 0, userId);

            return roles != null ? roles : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching club roles from database", e);
            return Collections.emptyList();
        }
    }

    // ============= RECRUITMENT-BASED CHECKS =============

    /**
     * Check if current user is a club officer of the club that owns the recruitment
     * @param recruitmentId ID of the recruitment
     * @return true if user is club officer of the recruitment's club, false otherwise
     */
    public boolean isClubOfficerForRecruitment(Long recruitmentId) {
        try {
            return recruitmentRepository.findById(recruitmentId)
                    .map(recruitment -> isClubOfficerInClub(recruitment.getClub().getId()))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking club officer permission for recruitment", e);
            return false;
        }
    }

    /**
     * Check if current user is a club officer of the club that owns the application's recruitment
     * @param applicationId ID of the recruitment application
     * @return true if user is club officer of the application's recruitment's club, false otherwise
     */
    public boolean isClubOfficerForApplication(Long applicationId) {
        try {
            return recruitmentApplicationRepository.findById(applicationId)
                    .map(application -> isClubOfficerInClub(application.getRecruitment().getClub().getId()))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking club officer permission for application", e);
            return false;
        }
    }

    // ============= REPORT-BASED CHECKS =============

    /**
     * Check if current user is a team officer or club officer of the club that owns the report
     * @param reportId ID of the report
     * @return true if user is team officer or club officer of the report's club, false otherwise
     */
    public boolean isTeamOfficerOrClubOfficerForReport(Long reportId) {
        try {
            return reportRepository.findById(reportId)
                    .map(report -> {
                        Long clubId = report.getClubReportRequirement().getClub().getId();
                        return isTeamOfficerOrClubOfficerOrTreasurerInClub(clubId);
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking team officer or club officer permission for report", e);
            return false;
        }
    }

    /**
     * Check if current user is a club officer of the club that owns the report
     * @param reportId ID of the report
     * @return true if user is club officer of the report's club, false otherwise
     */
    public boolean isClubOfficerForReport(Long reportId) {
        try {
            return reportRepository.findById(reportId)
                    .map(report -> {
                        Long clubId = report.getClubReportRequirement().getClub().getId();
                        return isClubOfficerInClub(clubId);
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking club officer permission for report", e);
            return false;
        }
    }

    /**
     * Check if current user is a member of the club that owns the report
     * @param reportId ID of the report
     * @return true if user is member of the report's club, false otherwise
     */
    public boolean isMemberOfClubForReport(Long reportId) {
        try {
            return reportRepository.findById(reportId)
                    .map(report -> {
                        Long clubId = report.getClubReportRequirement().getClub().getId();
                        return isMemberOfClub(clubId);
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking member permission for report", e);
            return false;
        }
    }
}
