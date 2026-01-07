package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SpotlightDTO {
    private String type;
    private String title;
    private String description;
    private String imageUrl;
    private String callToActionText;
    private String callToActionLink;
}
