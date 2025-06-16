package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedRiderProfileDTO;

/**
 * Output port for rider service operations.
 * This port defines the contract for validating riders through external service.
 */
public interface ApplicationRiderServiceOutputPort {
    
    /**
     * Validates a rider and returns their profile information.
     * 
     * @param riderId The rider ID to validate
     * @return SharedRiderProfileDTO containing rider profile information
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException if service is unavailable
     */
    SharedRiderProfileDTO validateRider(String riderId);
}