package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubProposalResponse {
    private Long id;
    private String title;
    private String fileUrl;
    private Long requestEstablishmentId;
    private Long clubId; // null nếu chưa có club
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

