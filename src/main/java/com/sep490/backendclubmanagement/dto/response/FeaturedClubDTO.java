// In: dto/response/FeaturedClubDTO.java
package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeaturedClubDTO {
    private Long id;
    private String clubName;
    private String logoUrl;
    private String description;
}