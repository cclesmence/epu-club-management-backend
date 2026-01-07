package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RecruitmentQuestionRequest {
    public Long id; // null => create; not null => update
    @NotBlank
    public String questionText;
    @NotBlank
    public String questionType;
    @NotNull
    public Integer questionOrder;
    public Integer isRequired; // 0 = not required, 1 = required
    public List<String> options; // optional for select-type questions
}