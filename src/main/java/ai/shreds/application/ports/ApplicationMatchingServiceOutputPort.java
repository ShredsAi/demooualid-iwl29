package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedMatchingRequestDTO;

/**
 * Output port for matching service operations.
 * This port defines the contract for requesting driver matching through external service.
 */
public interface ApplicationMatchingServiceOutputPort {
    
    /**
     * Requests driver matching for a trip.
     * 
     * @param request The matching request containing trip details
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureMessagingException if messaging fails
     */
    void requestMatching(SharedMatchingRequestDTO request);
}