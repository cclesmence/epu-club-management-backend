package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String studentCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatarUrl;
    private Boolean isActive;

    private Long systemRoleId;
    private String systemRoleName;

    // Danh sách CLB mà user tham gia
    private List<ClubMembershipProfileResponse> clubMemberships;
}
