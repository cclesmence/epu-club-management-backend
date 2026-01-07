package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClubCategoryRequest {

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 100, message = "Tên thể loại không được vượt quá 100 ký tự")
    private String categoryName;
}

