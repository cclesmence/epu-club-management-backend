package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.ReportStatus;
import com.sep490.backendclubmanagement.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilterRequest extends PageableRequest {
    private ReportStatus status;
    private Long clubId;
    private Long semesterId;
    private ReportType reportType;
    private String keyword;
}

