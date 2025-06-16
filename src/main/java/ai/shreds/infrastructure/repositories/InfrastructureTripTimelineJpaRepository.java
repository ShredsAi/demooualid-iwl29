package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripTimelineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for TripTimeline entities.
 * Provides operations for managing trip timelines and their metadata.
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
     * Check if timeline exists for a trip
     * @param tripId Trip ID
     * @return true if timeline exists
     */
    @Query("SELECT COUNT(t) > 0 FROM DomainTripTimelineJpaEntity t WHERE t.tripId = :tripId")
    boolean existsByTripId(@Param("tripId") UUID tripId);
    
    /**
     * Find timelines created after a specific date
     * @param createdAfter Date threshold
     * @return List of timelines created after the date
     */
    @Query("SELECT t FROM DomainTripTimelineJpaEntity t WHERE t.createdAt >= :createdAfter ORDER BY t.createdAt DESC")
    List<DomainTripTimelineJpaEntity> findTimelinesCreatedAfter(@Param("createdAfter") OffsetDateTime createdAfter);
    
    /**
     * Find timelines by multiple trip IDs
     * @param tripIds List of trip IDs
     * @return List of matching timelines
     */
    @Query("SELECT t FROM DomainTripTimelineJpaEntity t WHERE t.tripId IN :tripIds")
    List<DomainTripTimelineJpaEntity> findByTripIdIn(@Param("tripIds") List<UUID> tripIds);
    
    /**
     * Count total timelines
     * @return Total count of timelines
     */
    @Query("SELECT COUNT(t) FROM DomainTripTimelineJpaEntity t")
    long countAllTimelines();
    
    /**
     * Delete timeline by trip ID
     * @param tripId Trip ID
     */
    @Modifying
    @Query("DELETE FROM DomainTripTimelineJpaEntity t WHERE t.tripId = :tripId")
    void deleteByTripId(@Param("tripId") UUID tripId);
}