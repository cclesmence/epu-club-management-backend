package com.sep490.backendclubmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PublicClubCardDTO {
    private Long id;
    private String clubCode;
    private String clubName;
    private String shortDescription;
    private String bannerUrl;
    private String logoUrl;
    private boolean featured;
    private LocalDateTime createdAt;
    private String categoryName;
    private String campusName;

    private Long totalTeams;
    private List<String> topTags;
    private Integer tagsOverflow;
    private String leaderName;
    private Integer memberCount;
    
    private Boolean hasActiveRecruitment;
    private Long activeRecruitmentId;
}
