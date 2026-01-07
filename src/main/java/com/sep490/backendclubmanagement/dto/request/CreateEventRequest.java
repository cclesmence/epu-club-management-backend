package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String location;
    
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
    
    // Optional: STAFF can create events without a club
    private Long clubId;
    
    private Long eventTypeId;
    
    private List<MultipartFile> mediaFiles;
}

