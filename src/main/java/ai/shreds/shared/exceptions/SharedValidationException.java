package ai.shreds.shared.exceptions;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown when validation errors occur on input data.
 * Contains field-specific error messages for detailed feedback.
 */
@Getter
public class SharedValidationException extends RuntimeException {

    /** Map of field names to their specific validation error messages. */
    private final Map<String, String> fieldErrors;

    /**
     * Creates a validation exception with a general message and field-specific errors.
     *
     * @param message general validation error message
     * @param fieldErrors map of field names to their error messages
     */
    public SharedValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    /**
     * Creates a validation exception with only a general message.
     *
     * @param message general validation error message
     */
    public SharedValidationException(String message) {
        this(message, Collections.emptyMap());
    }

    /**
     * Gets the field-specific validation errors.
     *
     * @return map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
}