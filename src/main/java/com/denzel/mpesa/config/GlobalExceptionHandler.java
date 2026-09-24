package com.denzel.mpesa.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised error handling. Returns JSON error responses that mirror
 * the structure Safaricom uses in Daraja error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Validation failures → 400 with field-level detail */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(Map.of(
                "requestId", "N/A",
                "errorCode", "400.002.02",
                "errorMessage", "Bad Request - Invalid request",
                "fieldErrors", fieldErrors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /** Catch-all → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "errorCode", "500.001.01",
                "errorMessage", "Internal server error",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
