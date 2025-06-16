package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripTimelineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for TripTimeline entities.
 */
@Repository
public interface InfrastructureTimelineJpaRepository extends JpaRepository<DomainTripTimelineJpaEntity, UUID> {
    
    /**
     * Find timeline by trip ID
     * @param tripId Trip ID
     * @return Optional containing the timeline if found
     */
    Optional<DomainTripTimelineJpaEntity> findByTripId(UUID tripId);
    
    /**
     * Find timeline by ID
     * @param id Timeline ID
     * @return Optional containing the timeline if found
     */
    Optional<DomainTripTimelineJpaEntity> findById(UUID id);
    
    /**
     * Save timeline entity
     * @param entity Timeline entity to save
     * @return Saved timeline entity
     */
    DomainTripTimelineJpaEntity save(DomainTripTimelineJpaEntity entity);
    
    /**
     * Check if timeline exists for a trip
     * @param tripId Trip ID
     * @return true if timeline exists
     */
    @Query("SELECT COUNT(t) > 0 FROM DomainTripTimelineJpaEntity t WHERE t.tripId = :tripId")
    boolean existsByTripId(@Param("tripId") UUID tripId);
}