package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleInClubResponse {

    private Long roleMembershipId;

    private Long clubRoleId;
    private String clubRoleName;
    private String clubRoleCode;
    private Integer clubRoleLevel;

    private Long teamId;
    private String teamName;
    private Long semesterId;
    private String semesterName;
    private Boolean semesterIsCurrent;
    private Boolean isActive;
}
