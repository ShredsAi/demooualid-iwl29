package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when external services are unavailable or return errors
 */
public class InfrastructureServiceUnavailableException extends RuntimeException {

    private final String serviceName;
    private final Integer httpStatus;

    public InfrastructureServiceUnavailableException(String serviceName, Integer httpStatus) {
        super(String.format("Service '%s' is unavailable or returned error status: %d", 
                serviceName, httpStatus));
        this.serviceName = serviceName;
        this.httpStatus = httpStatus;
    }

    public InfrastructureServiceUnavailableException(String serviceName, Integer httpStatus, String message) {
        super(String.format("Service '%s' is unavailable (HTTP %d): %s", 
                serviceName, httpStatus, message));
        this.serviceName = serviceName;
        this.httpStatus = httpStatus;
    }

    public InfrastructureServiceUnavailableException(String serviceName, Integer httpStatus, Throwable cause) {
        super(String.format("Service '%s' is unavailable (HTTP %d): %s", 
                serviceName, httpStatus, cause.getMessage()), cause);
        this.serviceName = serviceName;
        this.httpStatus = httpStatus;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}