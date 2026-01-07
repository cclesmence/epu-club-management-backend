package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String gender;
    private String avatarUrl;
    private Boolean isActive;
}



