package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMemberStatusRequest {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
    
    private Long semesterId; // Optional: if not provided, use current semester
}



