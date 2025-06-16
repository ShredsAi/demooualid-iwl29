package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationCreateTripCommand;
import ai.shreds.application.dtos.ApplicationTripDTO;

/**
 * Input port for creating trips.
 * This port defines the contract for trip creation operations.
 */
public interface ApplicationCreateTripInputPort {
    
    /**
     * Creates a new trip based on the provided command.
     * 
     * @param command The command containing trip creation details
     * @return ApplicationTripDTO containing the created trip information
     * @throws ai.shreds.application.exceptions.ApplicationRiderIneligibleException if rider is not eligible
     * @throws ai.shreds.application.exceptions.ApplicationPaymentFailedException if payment authorization fails
     * @throws ai.shreds.application.exceptions.ApplicationServiceException for general service errors
     */
    ApplicationTripDTO createTrip(ApplicationCreateTripCommand command);
}