package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PublicClubDetailDTO {
    private Long id;
    private String clubCode;
    private String clubName;
    private String description;
    private String bannerUrl;
    private String logoUrl;

    // contact + social
    private String email;
    private String phone;
    private String fbUrl;
    private String igUrl;
    private String ttUrl;
    private String ytUrl;

    private String campusName;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean featured;

    private Long departmentsCount;
    private Integer establishedYear;

    private List<PublicTeamDTO> teams;
    private List<PublicEventDTO> events;
    private List<PublicNewsDTO> news;
}
