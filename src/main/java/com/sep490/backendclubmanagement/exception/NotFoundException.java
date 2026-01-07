package com.sep490.backendclubmanagement.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String errorMsg) {
        super(errorMsg);
    }
}
