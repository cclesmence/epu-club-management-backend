package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchMarkAttendanceRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotEmpty(message = "Attendance list cannot be empty")
    @Valid
    private List<AttendanceItem> attendances;
    
    @Data
    public static class AttendanceItem {
        @NotNull(message = "User ID is required")
        private Long userId;
        
        @NotNull(message = "Attendance Status is required")
        private AttendanceStatus attendanceStatus;
        
        private String notes;
    }
}




