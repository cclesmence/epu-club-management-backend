package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.RequestEstablishmentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestEstablishmentResponse {
    private Long id;
    private String clubName;
    private String clubCategory;
    private String clubCode;
    private RequestEstablishmentStatus status;
    private LocalDateTime sendDate;
    private Integer expectedMemberCount;
    private String activityObjectives;
    private String expectedActivities;
    private String description;
    private String email; // Email liên hệ
    private String phone; // Số điện thoại liên hệ
    private String facebookLink; // Link Facebook
    private String instagramLink; // Link Instagram
    private String tiktokLink; // Link TikTok
    private LocalDateTime confirmationDeadline;
    private LocalDateTime receivedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Created by info
    private Long createdByUserId;
    private String createdByFullName;
    private String createdByEmail;
    private String createdByStudentCode;
    private String createdByAvatarUrl;

    // Assigned staff info
    private Long assignedStaffId;
    private String assignedStaffFullName;
    private String assignedStaffEmail;
}

