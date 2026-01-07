package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMemberRoleRequest {
    @NotNull(message = "roleId is required")
    private Long roleId;
    
    private Long semesterId; // Optional: if not provided, use current semester
}



