package com.github.meeting_platform.common.exceptions;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses across the application.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
                        IllegalArgumentException ex, WebRequest request) {
                log.warn("Illegal argument: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(MeetingNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleMeetingNotFoundException(
                        MeetingNotFoundException ex, WebRequest request) {
                log.warn("Meeting not found: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(SessionNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleSessionNotFoundException(
                        SessionNotFoundException ex, WebRequest request) {
                log.warn("Session not found: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(SessionEndedException.class)
        public ResponseEntity<ErrorResponse> handleSessionEndedException(
                        SessionEndedException ex, WebRequest request) {
                log.warn("Session ended: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex, WebRequest request) {
                String errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation error: {}", errors);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Failed",
                                errors,
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolationException(
                        ConstraintViolationException ex, WebRequest request) {
                String errors = ex.getConstraintViolations()
                                .stream()
                                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Constraint violation: {}", errors);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Failed",
                                errors,
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
                        MethodArgumentTypeMismatchException ex, WebRequest request) {
                log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                String.format("Invalid value for parameter '%s': %s", ex.getName(), ex.getMessage()),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(InvalidEventException.class)
        public ResponseEntity<ErrorResponse> handleInvalidEventException(
                        InvalidEventException ex, WebRequest request) {
                log.warn("Invalid event: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
                        org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
                log.warn("Data integrity violation: {}", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                "Duplicate entry detected. This operation is idempotent and the resource already exists.",
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, WebRequest request) {
                log.error("Unexpected error: ", ex);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "An unexpected error occurred. Please try again later.",
                                request.getDescription(false).replace("uri=", ""));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
