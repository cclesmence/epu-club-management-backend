package com.sep490.backendclubmanagement.exception;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@Log4j2
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {



    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode code = ex.getErrorCode();

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();


        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiResponse.FieldError(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .toList();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException ex) {
        log.warn("Multipart error (likely file size exceeded): {}", ex.getMessage());
        
        String message = "Dung lượng file quá lớn. Kích thước tối đa cho phép là 20MB.";
        // Check if it's a size-related error
        if (ex.getMessage() != null && (ex.getMessage().contains("size") || ex.getMessage().contains("exceeded"))) {
            message = "Dung lượng file quá lớn. Kích thước tối đa cho phép là 20MB.";
        } else {
            message = "Lỗi khi upload file: " + (ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi");
        }
        
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);


        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, null));
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.NOT_FOUND, ex.getMessage(), null));
    }
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());

        return ResponseEntity
                .status(403)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN, ex.getMessage(), null));
    }

    /**
     * Handle AuthenticationException thrown from controllers/services
     * Note: Filter-level authentication failures are handled by JwtAuthenticationEntryPoint
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity
                .status(401)
                .body(ApiResponse.error(ErrorCode.UNAUTHENTICATED, null));
    }

    /**
     * Handle AccessDeniedException from Spring Security (e.g., @PreAuthorize failures)
     * This catches authorization failures at the method level
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(403)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN, null));
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.TEAM_NAME_EXISTED.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.TEAM_NAME_EXISTED, ex.getMessage(), null));
    }



}
