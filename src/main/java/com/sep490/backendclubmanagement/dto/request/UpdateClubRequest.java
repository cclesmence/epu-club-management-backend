package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateClubRequest {

    @NotBlank(message = "Tên câu lạc bộ không được để trống")
    private String clubName;
    @NotBlank(message = "Mã câu lạc bộ không được để trống")
    private String clubCode;

    private String description;

    private String status;

    private Long campusId;

    private Long categoryId;
}

