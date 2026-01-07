package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMembershipProfileResponse {

    private Long clubMembershipId;

    private Long clubId;
    private String clubName;
    private String clubCode;
    private String clubLogoUrl;
    private String clubStatus;
    private Boolean clubFeatured;

    private LocalDate joinDate;
    private LocalDate endDate;
    private ClubMemberShipStatus membershipStatus;

    // Các vai trò của user trong CLB này (theo RoleMemberShip)
    private List<RoleInClubResponse> roles;
}

