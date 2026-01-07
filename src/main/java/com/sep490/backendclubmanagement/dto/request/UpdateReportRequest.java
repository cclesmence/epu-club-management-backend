package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReportRequest {
    
    @NotBlank(message = "Report title is required")
    private String reportTitle;
    
    private String content;
    
    private String fileUrl;
}

