package com.sep490.backendclubmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCampusRequest {
    private String campusName;
    private String campusCode;
    private String address;
    private String phone;
    private String email;
}


