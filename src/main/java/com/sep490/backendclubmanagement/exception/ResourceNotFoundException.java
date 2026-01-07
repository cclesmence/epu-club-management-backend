package com.sep490.backendclubmanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception tùy chỉnh được ném ra khi không tìm thấy một tài nguyên cụ thể.
 * Annotation @ResponseStatus sẽ khiến Spring Boot tự động trả về mã lỗi 404 NOT FOUND.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
