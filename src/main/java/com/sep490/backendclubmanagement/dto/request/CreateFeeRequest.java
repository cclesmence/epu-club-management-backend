package com.sep490.backendclubmanagement.dto.request;

import com.sep490.backendclubmanagement.entity.fee.FeeType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateFeeRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull
    private FeeType feeType; // MEMBERSHIP, EVENT, OTHER

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private Boolean isMandatory;

    private Boolean isDraft;

    private Long semesterId; // For MEMBERSHIP fee type
}

