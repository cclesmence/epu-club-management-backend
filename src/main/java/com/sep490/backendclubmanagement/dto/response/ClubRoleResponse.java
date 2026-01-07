package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRoleResponse {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer roleLevel;
    private Long systemRoleId;
    private String systemRoleName;
}
