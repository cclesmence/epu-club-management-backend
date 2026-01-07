package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateClubRoleRequest {

    @NotBlank
    private String roleName;

    @NotBlank
    private String roleCode;

    private String description;

    @NotNull
    private Integer roleLevel;

    private Long systemRoleId; // có thể null
}
