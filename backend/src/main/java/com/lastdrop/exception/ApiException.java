package com.lastdrop.exception;

import org.springframework.http.HttpStatus;

/**
 * Throw this from a service to stop the request with a clear message,
 * e.g.  throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
 * GlobalExceptionHandler turns it into a JSON error response.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
