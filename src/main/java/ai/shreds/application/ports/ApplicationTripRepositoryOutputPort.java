package ai.shreds.application.ports;

import ai.shreds.domain.entities.DomainTripEntity;

import java.util.Optional;

/**
 * Output port for trip repository operations.
 * This port defines the contract for persisting and retrieving trip entities.
 */
public interface ApplicationTripRepositoryOutputPort {
    
    /**
     * Saves a trip entity.
     * 
     * @param trip The trip entity to save
     * @return The saved trip entity
     */
    DomainTripEntity save(DomainTripEntity trip);
    
    /**
     * Finds a trip by its ID.
     * 
     * @param tripId The trip ID to search for
     * @return Optional containing the trip if found, empty otherwise
     */
    Optional<DomainTripEntity> findById(String tripId);
    
    /**
     * Updates an existing trip entity.
     * 
     * @param trip The trip entity to update
     * @return The updated trip entity
     */
    DomainTripEntity update(DomainTripEntity trip);
}