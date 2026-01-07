package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProposeDefenseScheduleRequest {

    @NotNull(message = "Ngày giờ bảo vệ không được để trống")
    private LocalDateTime defenseDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime defenseEndDate;

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    private String meetingLink; // Optional

    private String notes; // Optional
}

