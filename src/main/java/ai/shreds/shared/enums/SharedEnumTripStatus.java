package ai.shreds.shared.enums;

/**
 * Enum representing the possible states of a trip.
 * This is shared across the system to maintain consistent trip status representation.
 */
public enum SharedEnumTripStatus {
    /**
     * Trip has been requested by the rider but matching hasn't started
     */
    REQUESTED,
    
    /**
     * Trip is in the process of matching with a driver
     */
    MATCHING,
    
    /**
     * Trip has been successfully matched with a driver
     */
    MATCHED,
    
    /**
     * Trip was cancelled by rider, driver, or system
     */
    CANCELLED,
    
    /**
     * Trip matching process failed to find a suitable driver
     */
    MATCH_FAILED
}