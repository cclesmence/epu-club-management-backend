package com.sep490.backendclubmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampusRequest {
    private String campusName;
    private String campusCode;
    private String address;
    private String phone;
    private String email;
}

