package com.sep490.backendclubmanagement.dto.websocket;

import com.sep490.backendclubmanagement.entity.RequestEstablishmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubCreationWebSocketPayload {
    private Long requestId;
    private String clubName;
    private RequestEstablishmentStatus status;
    private Long assignedStaffId;
    private String assignedStaffName;
    private String assignedStaffEmail;
    private Long creatorId;
    private String creatorName;
    private String creatorEmail;
    private LocalDateTime deadline;
    private String reason;
    private String comment;
    private String message;
    
    // Proposal related
    private Long proposalId;
    private String proposalTitle;
    
    // Defense schedule related
    private Long defenseScheduleId;
    private LocalDateTime defenseDate;
    private LocalDateTime defenseEndDate;
    private String location;
    private String meetingLink;
    private String defenseResult; // PASSED, FAILED
    private String feedback;
    
    // Final form related
    private Long finalFormId;
    private String finalFormTitle;
    
    // Club created
    private Long clubId;
    private String clubCode;
}

