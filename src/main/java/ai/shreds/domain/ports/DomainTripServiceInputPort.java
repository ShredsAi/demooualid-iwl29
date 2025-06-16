package ai.shreds.domain.ports;

import ai.shreds.domain.commands.*;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.domain.value_objects.DomainTripIdValue;

public interface DomainTripServiceInputPort {
    
    /**
     * Creates a new trip based on the provided command
     * @param command The create trip command containing all necessary information
     * @return The created trip entity
     */
    DomainTripEntity createTrip(DomainCreateTripCommand command);
    
    /**
     * Creates a new trip with a predefined trip ID
     * @param tripId The predefined trip ID to use
     * @param command The create trip command containing all necessary information
     * @return The created trip entity
     */
    DomainTripEntity createTripWithId(DomainTripIdValue tripId, DomainCreateTripCommand command);
    
    /**
     * Updates the status of an existing trip
     * @param tripId The trip identifier
     * @param newStatus The new status to set
     * @return The updated trip entity
     */
    DomainTripEntity updateTripStatus(String tripId, DomainTripStatusEnum newStatus);
    
    /**
     * Assigns a driver to a trip
     * @param command The assign driver command
     * @return The updated trip entity
     */
    DomainTripEntity assignDriver(DomainAssignDriverCommand command);
    
    /**
     * Cancels a trip
     * @param command The cancel trip command
     * @return The cancelled trip entity
     */
    DomainTripEntity cancelTrip(DomainCancelTripCommand command);
    
    /**
     * Completes a trip with final fare
     * @param tripId The trip identifier
     * @param finalFare The final fare amount
     * @return The completed trip entity
     */
    DomainTripEntity completeTrip(String tripId, DomainMoneyValue finalFare);
    
    /**
     * Handles matching timeout for a trip
     * @param command The matching timeout command
     * @return The updated trip entity
     */
    DomainTripEntity handleMatchingTimeout(DomainMatchingTimeoutCommand command);
    
    /**
     * Handles internal timeout for a trip
     * @param command The timeout command
     * @return The updated trip entity
     */
    DomainTripEntity handleInternalTimeout(DomainTimeoutCommand command);
}