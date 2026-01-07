package com.sep490.backendclubmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffRequest {
    private String email;
    private String fullName;
    private String phoneNumber;
    private String gender;
    private String studentCode;
    private Boolean isActive;
}


