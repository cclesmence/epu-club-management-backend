package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMemberTeamRequest {
    @NotNull(message = "teamId is required")
    private Long teamId;
    
    private Long semesterId; // Optional: if not provided, use current semester
}



