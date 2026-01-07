package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;

@Data
public class AdminDepartmentUpdateRequest {

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
    private Long campusId;
}
