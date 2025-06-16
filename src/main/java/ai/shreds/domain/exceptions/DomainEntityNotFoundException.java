package ai.shreds.domain.exceptions;

public class DomainEntityNotFoundException extends RuntimeException {
    private final String entityType;
    private final String entityId;

    public DomainEntityNotFoundException(String entityType, String entityId) {
        super(String.format("%s with ID %s not found", entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
