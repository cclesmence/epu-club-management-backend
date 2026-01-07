package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRegistrationDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String studentCode;
    private String email;
    private String avatarUrl;
    private LocalDateTime registrationTime;
    private String attendanceStatus;
    private LocalDateTime checkInTime;
    private String notes;
}
















