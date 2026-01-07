package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.fee.FeeType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeeResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private FeeType feeType;
    private LocalDate dueDate;
    private Boolean isMandatory;
    private Long semesterId; // For MEMBERSHIP fee type
    private String semesterName; // For display
}
