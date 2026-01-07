package com.sep490.backendclubmanagement.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String errorMsg) {
        super(errorMsg);
    }
}

