package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusSimpleResponse {

    private Long id;
    private String campusCode;
    private String campusName;
    private String address;
    private String email;
    private String phone;
}
