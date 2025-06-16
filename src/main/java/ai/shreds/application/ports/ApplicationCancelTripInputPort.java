package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationCancelTripCommand;
import ai.shreds.application.dtos.ApplicationCancellationResultDTO;

/**
 * Input port for canceling trips.
 * This port defines the contract for trip cancellation operations.
 */
public interface ApplicationCancelTripInputPort {
    
    /**
     * Cancels a trip based on the provided command.
     * 
     * @param command The command containing trip cancellation details
     * @return ApplicationCancellationResultDTO containing the cancellation result
     * @throws ai.shreds.shared.exceptions.SharedNotFoundException if trip is not found
     * @throws ai.shreds.shared.exceptions.SharedConflictException if trip cannot be cancelled
     * @throws ai.shreds.application.exceptions.ApplicationServiceException for general service errors
     */
    ApplicationCancellationResultDTO cancelTrip(ApplicationCancelTripCommand command);
}