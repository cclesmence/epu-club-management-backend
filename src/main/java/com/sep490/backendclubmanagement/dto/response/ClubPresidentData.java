package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubPresidentData {
    private String fullName;
    private String email;
    private String avatarUrl;
}
