package ai.shreds.shared.exceptions;

import lombok.Getter;

/**
 * Exception thrown when an operation conflicts with the current state
 * of a resource (e.g., trying to cancel an already completed trip).
 */
@Getter
public class SharedConflictException extends RuntimeException {

    /** Reason for the conflict. */
    private final String conflictReason;

    /** Current state of the resource that caused the conflict. */
    private final String currentState;

    /**
     * Creates a conflict exception with detailed information.
     *
     * @param message general error message
     * @param conflictReason specific reason for the conflict
     * @param currentState current state of the resource
     */
    public SharedConflictException(String message, String conflictReason, String currentState) {
        super(message);
        this.conflictReason = conflictReason;
        this.currentState = currentState;
    }

    /**
     * Gets the reason for the conflict.
     *
     * @return conflict reason
     */
    public String getConflictReason() {
        return conflictReason;
    }

    /**
     * Gets the current state that caused the conflict.
     *
     * @return current state
     */
    public String getCurrentState() {
        return currentState;
    }
}