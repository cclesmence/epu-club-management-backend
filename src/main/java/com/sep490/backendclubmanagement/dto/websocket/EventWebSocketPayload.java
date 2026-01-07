package com.sep490.backendclubmanagement.dto.websocket;

import com.sep490.backendclubmanagement.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventWebSocketPayload {
    private Long eventId;
    private String eventTitle;
    private Long requestEventId;
    private RequestStatus status;
    private Long clubId;
    private String clubName;
    private Long creatorId;
    private String creatorName;
    private String creatorEmail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String eventTypeName;
    private String responseMessage;
    private String reason;
    private String message;
    private Long approverId;
    private String approverName;
    private String approverRole;
}

