package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRoleInfo {
    private Long clubId;
    private String clubName;
    private String clubRole; // roleName từ ClubRole (ví dụ: "thành viên", "Chủ nhiệm")
    private String systemRole; // roleCode từ ClubRole (ví dụ: "MEMBER", "CLUB_PRESIDENT")
}

