package ai.shreds.adapters.exceptions;

import ai.shreds.shared.exceptions.*;
import ai.shreds.application.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class AdapterExceptionHandler {

    @ExceptionHandler(SharedValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(SharedValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());
        ErrorResponse errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            ex.getFieldErrors()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(SharedNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(SharedNotFoundException ex) {
        log.warn("Resource not found: {} with ID: {}", ex.getResourceType(), ex.getResourceId());
        Map<String, String> details = new HashMap<>();
        details.put("resourceType", ex.getResourceType());
        details.put("resourceId", ex.getResourceId());
        
        ErrorResponse errorResponse = createErrorResponse(
            "RESOURCE_NOT_FOUND",
            String.format("%s with ID %s not found", ex.getResourceType(), ex.getResourceId()),
            details
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SharedConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(SharedConflictException ex) {
        log.warn("Conflict exception: {}", ex.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("conflictReason", ex.getConflictReason());
        details.put("currentState", ex.getCurrentState());
        
        ErrorResponse errorResponse = createErrorResponse(
            "CONFLICT_ERROR",
            ex.getMessage(),
            details
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(SharedPaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(SharedPaymentException ex) {
        log.error("Payment exception for rider {}: {}", ex.getRiderId(), ex.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("paymentError", ex.getPaymentError());
        details.put("riderId", ex.getRiderId());
        
        ErrorResponse errorResponse = createErrorResponse(
            "PAYMENT_ERROR",
            ex.getMessage(),
            details
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
    }

    @ExceptionHandler(ApplicationRiderIneligibleException.class)
    public ResponseEntity<ErrorResponse> handleRiderIneligibleException(ApplicationRiderIneligibleException ex) {
        log.warn("Rider ineligible: {} - reason: {}", ex.getRiderId(), ex.getReason());
        Map<String, String> details = new HashMap<>();
        details.put("riderId", ex.getRiderId());
        details.put("reason", ex.getReason());
        
        ErrorResponse errorResponse = createErrorResponse(
            "RIDER_INELIGIBLE",
            "Rider is not eligible to request trips",
            details
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ApplicationPaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailedException(ApplicationPaymentFailedException ex) {
        log.error("Payment failed for rider {}: {}", ex.getRiderId(), ex.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("riderId", ex.getRiderId());
        details.put("errorCode", ex.getErrorCode());
        details.put("amount", ex.getAmount().toString());
        
        ErrorResponse errorResponse = createErrorResponse(
            "PAYMENT_FAILED",
            "Payment authorization failed",
            details
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint validation failed: {}", ex.getMessage());
        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            violations.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        
        ErrorResponse errorResponse = createErrorResponse(
            "VALIDATION_ERROR",
            "Constraint validation failed",
            violations
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("parameter", ex.getName());
        details.put("value", String.valueOf(ex.getValue()));
        details.put("expectedType", ex.getRequiredType().getSimpleName());
        
        ErrorResponse errorResponse = createErrorResponse(
            "INVALID_PARAMETER",
            "Invalid parameter type",
            details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        ErrorResponse errorResponse = createErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            new HashMap<>()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ErrorResponse createErrorResponse(String code, String message, Map<String, String> details) {
        return ErrorResponse.builder()
            .code(code)
            .message(message)
            .details(details)
            .timestamp(java.time.Instant.now().toString())
            .build();
    }

    public static class ErrorResponse {
        private String code;
        private String message;
        private String timestamp;
        private Map<String, String> details;

        public ErrorResponse() {}

        public ErrorResponse(String code, String message, String timestamp, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.details = details;
        }

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public Map<String, String> getDetails() { return details; }
        public void setDetails(Map<String, String> details) { this.details = details; }

        public static class ErrorResponseBuilder {
            private String code;
            private String message;
            private String timestamp;
            private Map<String, String> details;

            public ErrorResponseBuilder code(String code) {
                this.code = code;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ErrorResponseBuilder timestamp(String timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public ErrorResponseBuilder details(Map<String, String> details) {
                this.details = details;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(code, message, timestamp, details);
            }
        }
    }
}