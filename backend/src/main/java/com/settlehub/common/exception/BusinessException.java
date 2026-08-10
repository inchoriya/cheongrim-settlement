package com.settlehub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException("INVALID_STATE", message, HttpStatus.CONFLICT);
    }
}
