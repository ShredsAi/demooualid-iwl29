package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripJpaEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Trip entities.
 */
@Repository
public interface InfrastructureTripJpaRepository extends JpaRepository<DomainTripJpaEntity, UUID> {
    
    /**
     * Find a trip by its ID
     * @param id Trip ID
     * @return Optional containing the trip if found
     */
    Optional<DomainTripJpaEntity> findById(UUID id);
    
    /**
     * Find all trips with a specific status
     * @param status Trip status to filter by
     * @return List of trips with the given status
     */
    List<DomainTripJpaEntity> findByStatus(String status);
    
    /**
     * Find trips by rider ID and status
     * @param riderId Rider ID
     * @param status Trip status
     * @return List of trips matching criteria
     */
    List<DomainTripJpaEntity> findByRiderIdAndStatus(UUID riderId, String status);
}
