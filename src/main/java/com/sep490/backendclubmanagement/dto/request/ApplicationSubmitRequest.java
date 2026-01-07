package com.sep490.backendclubmanagement.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ApplicationSubmitRequest {
    @NotNull
    public Long recruitmentId;
    public Long teamId; // optional
    @NotNull
    public List<FormAnswerRequest> answers;

    public static class FormAnswerRequest {
        @NotNull
        public Long questionId;
        public String answerText;
        public Boolean hasFile; // Indicates whether this answer should use the uploaded file
    }
}


