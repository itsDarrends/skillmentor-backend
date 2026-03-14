package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.ErrorResponse;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AbstractController {

    protected <T> ResponseEntity<T> sendOkResponse(T response) {
        return ResponseEntity.ok(response);
    }

    protected <T> ResponseEntity<T> sendCreatedResponse(T response) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    protected <T> ResponseEntity<T> sendNotFoundResponse() {
        return ResponseEntity.notFound().build();
    }

    protected <T> ResponseEntity<T> sendNoContentResponse() {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        if (ex instanceof AccessDeniedException) {
            throw (AccessDeniedException) ex;
        }
        log.error("Unexpected error: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("An unexpected error occurred")
                .errorCode("INTERNAL SERVER ERROR")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SkillMentorException.class)
    public ResponseEntity<ErrorResponse> handleSkillMentorException(SkillMentorException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getStatus().toString())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .errorCode("BAD REQUEST")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .validationErrors(errors)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}