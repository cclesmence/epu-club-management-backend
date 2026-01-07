package com.sep490.backendclubmanagement.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubCreationFinalFormResponse {
    private Long id;
    private String formData; // JSON data
    private String status; // SUBMITTED, REVIEWED, APPROVED
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long requestEstablishmentId;
    
    // Submitted by info
    private Long submittedById;
    private String submittedByFullName;
    private String submittedByEmail;
    
    // Reviewed by info
    private Long reviewedById;
    private String reviewedByFullName;
    private String reviewedByEmail;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

