package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventData {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String startTime;
    private String endTime;
    private boolean isDraft;
    private Long clubId;
    private String clubName;
    private List<String> mediaUrls;
    private List<String> mediaTypes; // "IMAGE" or "VIDEO" - maps to mediaUrls by index
    private List<Long> mediaIds; // IDs của media - maps to mediaUrls by index
    private Long eventTypeId;
    private String eventTypeName;

}
