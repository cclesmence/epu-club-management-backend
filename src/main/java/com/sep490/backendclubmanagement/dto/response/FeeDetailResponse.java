package com.sep490.backendclubmanagement.dto.response;

import com.sep490.backendclubmanagement.entity.fee.FeeType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FeeDetailResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private FeeType feeType;
    private LocalDate dueDate;
    private Boolean isMandatory;
    private Boolean isDraft;
    private Boolean hasEverExpired; // Once true, amount cannot be edited
    private Integer paidMembers;
    private Integer totalMembers;
    private LocalDateTime paidDate;
    private String transactionReference;
    private Long semesterId; // For MEMBERSHIP fee type
    private String semesterName; // For display
}
