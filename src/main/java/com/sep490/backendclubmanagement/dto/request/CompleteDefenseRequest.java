package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.DefenseScheduleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteDefenseRequest {

    @NotNull(message = "Kết quả bảo vệ không được để trống")
    private DefenseScheduleStatus result; // PASSED hoặc FAILED

    private String feedback; // Optional feedback
}

