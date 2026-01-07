package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyDraftEventDto {
    private EventData event;
    private RequestStatus requestStatus;
}
