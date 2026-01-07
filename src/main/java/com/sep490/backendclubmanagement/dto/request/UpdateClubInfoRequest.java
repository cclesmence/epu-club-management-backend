package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClubInfoRequest {

    @NotBlank(message = "Tên câu lạc bộ không được để trống")
    private String clubName;

    @NotBlank(message = "Mã câu lạc bộ không được để trống")
    private String clubCode;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;

    private String logoUrl;

    private String bannerUrl;
    private long categoryId;

    private String email;

    private String phone;

    private String fbUrl;

    private String igUrl;

    private String ttUrl;

    private String ytUrl;

    private Boolean removeLogo;
    private Boolean removeBanner;
}

