package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffListResponse {
    @Builder.Default private long total = 0;
    @Builder.Default private long count = 0;
    @Builder.Default
    private List<StaffSummaryResponse> data = new ArrayList<>();
}



