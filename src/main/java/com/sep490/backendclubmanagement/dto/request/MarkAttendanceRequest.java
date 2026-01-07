package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkAttendanceRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Attendance Status is required")
    private AttendanceStatus attendanceStatus;
    
    private String notes;
}




