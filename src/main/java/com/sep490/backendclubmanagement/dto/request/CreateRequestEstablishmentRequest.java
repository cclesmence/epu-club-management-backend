package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRequestEstablishmentRequest {

    @NotBlank(message = "Tên CLB không được để trống")
    private String clubName;

    @NotBlank(message = "Danh mục CLB không được để trống")
    private String clubCategory;

    @NotNull(message = "Số lượng thành viên dự kiến không được để trống")
    @Positive(message = "Số lượng thành viên phải lớn hơn 0")
    private Integer expectedMemberCount;

    private String activityObjectives; // Mục tiêu hoạt động

    private String expectedActivities; // Hoạt động dự kiến

    private String description; // Mô tả CLB

    private String clubCode; // Mã CLB

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email; // Email liên hệ

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại phải có 10 số và bắt đầu bằng 0 hoặc +84")
    private String phone; // Số điện thoại liên hệ

    private String facebookLink; // Link Facebook (optional)

    private String instagramLink; // Link Instagram (optional)

    private String tiktokLink; // Link TikTok (optional)

    // true = lưu bản nháp (DRAFT), false = gửi yêu cầu (SUBMITTED)
    private Boolean isDraft = true;
}



