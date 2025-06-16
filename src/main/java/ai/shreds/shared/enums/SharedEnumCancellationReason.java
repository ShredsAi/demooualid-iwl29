package ai.shreds.shared.enums;

/**
 * Enum representing the possible reasons for trip cancellation.
 * This is shared across the system to maintain consistent cancellation reason representation.
 */
public enum SharedEnumCancellationReason {
    /**
     * Trip was cancelled by the rider
     */
    RIDER_CANCELLED,
    
    /**
     * Trip was cancelled by the driver
     */
    DRIVER_CANCELLED,
    
    /**
     * Trip was cancelled by the system due to technical or policy reasons
     */
    SYSTEM_CANCELLED,
    
    /**
     * Trip was cancelled due to payment authorization failure
     */
    PAYMENT_FAILED,
    
    /**
     * Trip was cancelled because no driver was found within the timeout period
     */
    MATCHING_TIMEOUT
}