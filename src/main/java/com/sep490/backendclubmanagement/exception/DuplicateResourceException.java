package com.sep490.backendclubmanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception cho lỗi tạo tài nguyên đã tồn tại (trùng tên, trùng email, ...).
 * Trả về HTTP 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
