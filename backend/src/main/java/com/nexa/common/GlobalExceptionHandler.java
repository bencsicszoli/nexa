package com.nexa.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Egységes hibaválasz: {@code { "code": "...", "message": "...", "fields": {...} }}.
 * A {@code code}-ot a frontend fordítja le a választott nyelvre (EN/HU).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Mezőnként a megsértett constraint kódja (pl. "Email", "NotBlank", "Size"),
            // ezt is a frontend fordítja, ha mezőszintű hibát akar mutatni.
            fields.putIfAbsent(fe.getField(), fe.getCode());
        }
        return ResponseEntity.badRequest()
                .body(body("VALIDATION_ERROR", "Request validation failed.", fields));
    }

    private Map<String, Object> body(String code, String message, Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (fields != null) {
            body.put("fields", fields);
        }
        return body;
    }
}
