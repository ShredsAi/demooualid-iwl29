package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationTimeoutCommand;

/**
 * Input port for handling internal timeout operations.
 * This port defines the contract for handling internal timer expiry events.
 */
public interface ApplicationTimeoutInputPort {
    
    /**
     * Handles internal timeout event when matching timer expires.
     * 
     * @param command The command containing timeout details
     * @throws ai.shreds.shared.exceptions.SharedNotFoundException if trip is not found
     * @throws ai.shreds.application.exceptions.ApplicationServiceException for general service errors
     */
    void handleTimeout(ApplicationTimeoutCommand command);
}