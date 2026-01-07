package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubCreationStepResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer orderIndex;
    private Boolean active;
}



