package com.example.hotelbooking.exception;

public class BusinessException extends RuntimeException {
    public enum Code {
        NOT_FOUND, NO_AVAILABILITY, INVALID_STATE, CANCELLATION_NOT_ALLOWED, REFUND_FAILED
    }

    private final Code code;

    public BusinessException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(Code.NOT_FOUND, resource + " not found");
    }
}
