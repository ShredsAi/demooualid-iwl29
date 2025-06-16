package ai.shreds.shared.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a requested resource cannot be found.
 * Contains information about the resource type and identifier.
 */
@Getter
public class SharedNotFoundException extends RuntimeException {

    /** Type of resource that was not found (e.g., "Trip", "Rider", "Driver"). */
    private final String resourceType;

    /** Identifier of the resource that was not found. */
    private final String resourceId;

    /**
     * Creates a not found exception with resource type and identifier.
     *
     * @param resourceType type of resource that was not found
     * @param resourceId identifier of the resource that was not found
     */
    public SharedNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Gets the type of resource that was not found.
     *
     * @return resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the identifier of the resource that was not found.
     *
     * @return resource identifier
     */
    public String getResourceId() {
        return resourceId;
    }
}