package com.sep490.backendclubmanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private Instant timestamp;
    private T data;
    private List<FieldError> errors;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldError {
        private String field;
        private String errorMessage;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .timestamp(Instant.now())
                .data(data)
                .build();
    }
    
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Success")
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage, List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(customMessage != null ? customMessage : errorCode.getMessage())
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

}
