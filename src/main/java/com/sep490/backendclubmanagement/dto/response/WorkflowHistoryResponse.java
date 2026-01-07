package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryResponse {
    private Long id;
    private LocalDateTime actionDate;
    private String comments;
    private LocalDateTime createdAt;
    
    // Step info
    private Long stepId;
    private String stepCode;
    private String stepName;
    private String stepDescription;
    
    // User info (acted by)
    private Long actedById;
    private String actedByFullName;
    private String actedByEmail;
    private String actedByStudentCode;
    private String actedByAvatarUrl;
}



