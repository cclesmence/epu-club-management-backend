package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubManagementResponse {
    private Long id;
    private String clubName;
    private String clubCode;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String email;
    private String phone;
    private String fbUrl;
    private String igUrl;
    private String ttUrl;
    private String ytUrl;
    private String status;
    private String campusName;
    private Long campusId;
    private String categoryName;
    private Long categoryId;
    private Long totalMembers;
    private Long totalEvents;
    private Long totalPosts;
    private List<ClubPresidentData> presidents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

