package com.sep490.backendclubmanagement.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveMemberRequest {
    // Optional reason for removal
    private String reason;
}



