package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRequestDto {
    private Long requestEventId;
    private String requestTitle;
    private RequestStatus status;
    private String responseMessage;
    private String description;
    private LocalDateTime requestDate;

    private EventSummaryDto event;
    private ClubMiniDto club;
    private UserMiniDto createdBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventSummaryDto {
        private Long id;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String location;
        private String eventTypeName;
        private Boolean isDraft;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClubMiniDto {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserMiniDto {
        private Long id;
        private String fullName;
    }
}


