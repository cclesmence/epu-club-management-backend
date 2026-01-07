package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {
    private Long id;
    private String teamName;
    private String description;
    private String linkGroupChat;
}
