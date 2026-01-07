package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClubCategoryDTO {
    private Long id;
    private String categoryName;
    // Số lượng câu lạc bộ thuộc thể loại này
    private Integer clubCount;
}
