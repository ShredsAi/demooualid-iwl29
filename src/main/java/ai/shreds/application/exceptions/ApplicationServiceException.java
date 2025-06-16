package ai.shreds.application.exceptions;

public class ApplicationServiceException extends RuntimeException {

    public ApplicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}