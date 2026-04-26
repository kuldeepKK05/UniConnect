package com.uniconnect.controllers;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// ─────────────────────────────────────────────────────────────
// Generic API response envelope
// ─────────────────────────────────────────────────────────────
@Data
class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private String  error;
    private long    timestamp;

    private ApiResponse() { this.timestamp = Instant.now().toEpochMilli(); }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data    = data;
        return r;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> r = success(data);
        r.message = message;
        return r;
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.error   = error;
        return r;
    }
}

// ─────────────────────────────────────────────────────────────
// Global exception handler
// ─────────────────────────────────────────────────────────────
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        ApiResponse<Void> resp = ApiResponse.error(ex.getReason() != null
                ? ex.getReason() : ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(resp);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = err instanceof FieldError ? ((FieldError) err).getField() : "error";
            String message = err.getDefaultMessage();
            errors.put(field, message);
        });
        ApiResponse<Map<String, String>> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setData(errors);
        resp.setError("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        ApiResponse<Void> resp = ApiResponse.error("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}
