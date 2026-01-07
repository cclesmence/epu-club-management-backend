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
public class CampusSummaryResponse {
    private Long id;
    private String campusName;
    private String campusCode;
    private String address;
    private String phone;
    private String email;
}


