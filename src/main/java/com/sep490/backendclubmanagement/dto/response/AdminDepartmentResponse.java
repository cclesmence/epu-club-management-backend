package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDepartmentResponse {

    private Long id;
    private String departmentCode;
    private String departmentName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bannerUrl;
    private String fbLink;
    private String igLink;
    private String ttLink;
    private String ytLink;
    private String sortDescription;

    private CampusSimpleResponse campus;
}
