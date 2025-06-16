package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedLocationDTO;
import ai.shreds.shared.dtos.SharedFareEstimateDTO;

import java.util.Map;

/**
 * Output port for pricing service operations.
 * This port defines the contract for fare estimation through external pricing service.
 */
public interface ApplicationPricingServiceOutputPort {
    
    /**
     * Estimates fare for a trip based on pickup and dropoff locations.
     * 
     * @param pickupLocation The pickup location
     * @param dropoffLocation The dropoff location
     * @param metadata Additional metadata for fare calculation
     * @return SharedFareEstimateDTO containing fare estimate details
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException if service is unavailable
     */
    SharedFareEstimateDTO estimateFare(SharedLocationDTO pickupLocation, 
                                       SharedLocationDTO dropoffLocation, 
                                       Map<String, String> metadata);
}