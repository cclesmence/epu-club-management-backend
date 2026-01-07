package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PublicTeamDTO {
    private Long id;
    private String teamName;
    private String description;
    private String linkGroupChat;
    private String leaderName;
    private Integer memberCount;
}
