package com.example.hotelbooking.advice;

import com.example.hotelbooking.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException exception) {
        var status = switch (exception.code()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case REFUND_FAILED -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status)
                .body(new ApiError(exception.code().name(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> validation(BindException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "Request validation failed", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformedRequest(Exception exception) {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_REQUEST", "Malformed request: check JSON, identifiers and date formats", Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, String> fieldErrors) {}
}
