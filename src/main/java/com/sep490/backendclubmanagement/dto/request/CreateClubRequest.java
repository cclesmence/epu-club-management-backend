package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClubRequest {

    @NotBlank(message = "Tên câu lạc bộ không được để trống")
    private String clubName;

    @NotBlank(message = "Mã câu lạc bộ không được để trống")
    private String clubCode;

    private String description;

    private String status;

    @NotNull(message = "Campus không được để trống")
    private Long campusId;

    @NotNull(message = "Danh mục câu lạc bộ không được để trống")
    private Long categoryId;

    @NotBlank(message = "Email chủ câu lạc bộ không được để trống")
    @Email(message = "Email không hợp lệ")
    private String presidentEmail;
}

