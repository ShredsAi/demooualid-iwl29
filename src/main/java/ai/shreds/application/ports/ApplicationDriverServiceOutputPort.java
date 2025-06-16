package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDriverProfileDTO;

/**
 * Output port for driver service operations.
 * This port defines the contract for validating drivers through external service.
 */
public interface ApplicationDriverServiceOutputPort {
    
    /**
     * Validates a driver and returns their profile information.
     * 
     * @param driverId The driver ID to validate
     * @return SharedDriverProfileDTO containing driver profile information
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException if service is unavailable
     */
    SharedDriverProfileDTO validateDriver(String driverId);
}