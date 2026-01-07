package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String roleName;
    private String email;
    private String studentCode;
}
