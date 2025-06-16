package ai.shreds.adapter.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ai.shreds.shared.exceptions.SharedValidationException;
import ai.shreds.shared.exceptions.SharedNotFoundException;
import ai.shreds.shared.exceptions.SharedConflictException;
import ai.shreds.shared.exceptions.SharedPaymentException;

import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class AdapterExceptionHandler {

    @ExceptionHandler(SharedValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(SharedValidationException ex) {
        ErrorResponse response = createErrorResponse("VALIDATION_ERROR", ex.getMessage(), ex.getFieldErrors());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(SharedNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(SharedNotFoundException ex) {
        Map<String, String> details = Collections.singletonMap("resource", ex.getResourceId());
        ErrorResponse response = createErrorResponse("NOT_FOUND", ex.getMessage(), details);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(SharedConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(SharedConflictException ex) {
        Map<String, String> details = Collections.singletonMap("conflictReason", ex.getConflictReason());
        ErrorResponse response = createErrorResponse("CONFLICT", ex.getMessage(), details);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(SharedPaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(SharedPaymentException ex) {
        Map<String, String> details = Collections.singletonMap("riderId", ex.getRiderId());
        ErrorResponse response = createErrorResponse("PAYMENT_ERROR", ex.getMessage(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = createErrorResponse("INTERNAL_ERROR", ex.getMessage(), Collections.emptyMap());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponse createErrorResponse(String code, String message, Map<String, String> details) {
        return new ErrorResponse(code, message, details);
    }

    public static class ErrorResponse {
        private String code;
        private String message;
        private Map<String, String> details;

        public ErrorResponse(String code, String message, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getDetails() {
            return details;
        }
    }
}
