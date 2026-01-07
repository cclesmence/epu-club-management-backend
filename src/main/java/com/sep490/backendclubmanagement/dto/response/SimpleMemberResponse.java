package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple member response for dropdown/select purposes
 * Used in fee assignment and other places that need basic member info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleMemberResponse {
    private Long userId;
    private String studentCode;
    private String fullName;
    private String email;
    private String avatarUrl;
}

