package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailableMemberDTO {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String email;
}
