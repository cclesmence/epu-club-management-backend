package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClubRoleRequest {

    @NotBlank
    private String roleName;

    // Optional - will be auto-generated from roleName if not provided
    private String roleCode;

    private String description;

    // Nên > 2 để không trùng với level Chủ nhiệm / Phó
    @NotNull
    private Integer roleLevel;

    // Có thể null nếu không gắn với system_roles
    private Long systemRoleId;
}
