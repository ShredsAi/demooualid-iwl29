package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationDriverAssignedCommand;
import ai.shreds.application.dtos.ApplicationMatchingTimeoutCommand;

/**
 * Input port for coordinating matching operations.
 * This port defines the contract for handling driver assignment and matching timeout events.
 */
public interface ApplicationMatchingCoordinatorInputPort {
    
    /**
     * Handles driver assigned event from matching service.
     * 
     * @param command The command containing driver assignment details
     * @throws ai.shreds.shared.exceptions.SharedNotFoundException if trip is not found
     * @throws ai.shreds.shared.exceptions.SharedConflictException if trip state is invalid
     * @throws ai.shreds.application.exceptions.ApplicationServiceException for general service errors
     */
    void handleDriverAssigned(ApplicationDriverAssignedCommand command);
    
    /**
     * Handles matching timeout event from matching service.
     * 
     * @param command The command containing timeout details
     * @throws ai.shreds.shared.exceptions.SharedNotFoundException if trip is not found
     * @throws ai.shreds.application.exceptions.ApplicationServiceException for general service errors
     */
    void handleMatchingTimeout(ApplicationMatchingTimeoutCommand command);
}