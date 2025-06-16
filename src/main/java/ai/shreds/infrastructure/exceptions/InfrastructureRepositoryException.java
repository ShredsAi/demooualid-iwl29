package ai.shreds.infrastructure.exceptions;

/**
 * Exception thrown when repository operations fail
 */
public class InfrastructureRepositoryException extends RuntimeException {

    private final String operation;
    private final String entityType;

    public InfrastructureRepositoryException(String operation, String entityType, Throwable cause) {
        super(String.format("Repository operation '%s' failed for entity type '%s': %s", 
                operation, entityType, cause.getMessage()), cause);
        this.operation = operation;
        this.entityType = entityType;
    }

    public InfrastructureRepositoryException(String operation, String entityType, String message) {
        super(String.format("Repository operation '%s' failed for entity type '%s': %s", 
                operation, entityType, message));
        this.operation = operation;
        this.entityType = entityType;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntityType() {
        return entityType;
    }
}