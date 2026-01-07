package com.sep490.backendclubmanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to be thrown when a user tries to access a resource
 * they do not have permission for.
 * The @ResponseStatus annotation will cause Spring Boot to respond with a
 * 403 FORBIDDEN status code whenever this exception is thrown and not caught.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
