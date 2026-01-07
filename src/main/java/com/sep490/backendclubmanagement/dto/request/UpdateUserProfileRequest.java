package com.sep490.backendclubmanagement.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequest {

    private String fullName;
    private String phoneNumber;
    private String studentCode;
    private LocalDate dateOfBirth;
    private String gender;

}