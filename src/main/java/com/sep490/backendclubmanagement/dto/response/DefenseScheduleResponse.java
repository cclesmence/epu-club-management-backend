package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.DefenseScheduleStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefenseScheduleResponse {
    private Long id;
    private LocalDateTime defenseDate;
    private LocalDateTime defenseEndDate;
    private String location;
    private String meetingLink;
    private String panelMembers;
    private String notes;
    private DefenseScheduleStatus result; // PASSED, FAILED, PENDING
    private String feedback;
    private String epuBookingId;
    private Boolean isAutoBooked;
    private String epuBookingStatus;
    private String epuBookingLink;
    private Long requestEstablishmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

