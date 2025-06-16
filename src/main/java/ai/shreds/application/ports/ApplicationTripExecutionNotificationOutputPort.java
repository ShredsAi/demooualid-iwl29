package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedTripMatchedMessageDTO;

/**
 * Output port for trip execution notification operations.
 * This port defines the contract for notifying the Trip Execution Shred when a trip is matched.
 */
public interface ApplicationTripExecutionNotificationOutputPort {
    
    /**
     * Notifies the Trip Execution Shred that a trip has been matched with a driver.
     * 
     * @param message The trip matched message containing trip and driver details
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureMessagingException if notification fails
     */
    void notifyTripMatched(SharedTripMatchedMessageDTO message);
}