package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOutcomeTransactionRequest {

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Ngày giao dịch không được để trống")
    private LocalDateTime transactionDate;

    @NotBlank(message = "Người nhận không được để trống")
    @Size(max = 200, message = "Người nhận không được vượt quá 200 ký tự")
    private String recipient;

    @NotBlank(message = "Mục đích không được để trống")
    @Size(max = 200, message = "Mục đích không được vượt quá 200 ký tự")
    private String purpose;

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String notes;

    @Size(max = 500, message = "URL biên lai không được vượt quá 500 ký tự")
    private String receiptUrl;
}

