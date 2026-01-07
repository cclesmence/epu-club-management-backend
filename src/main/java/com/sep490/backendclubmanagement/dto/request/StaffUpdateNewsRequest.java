package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffUpdateNewsRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @NotBlank(message = "Loại tin không được để trống")
    private String type; // bạn đang dùng tên field là newsType trong entity -> map ở service

    @NotBlank(message = "Ảnh đại diện (thumbnailUrl) không được để trống")
    private String thumbnailUrl;
}
