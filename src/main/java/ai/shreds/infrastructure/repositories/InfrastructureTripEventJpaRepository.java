package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for Trip Event entities.
 */
@Repository
public interface InfrastructureTripEventJpaRepository extends JpaRepository<DomainTripEventJpaEntity, Long> {
    
    /**
     * Find all events for a specific timeline
     * @param timelineId Timeline ID
     * @return List of events ordered by timestamp
     */
    List<DomainTripEventJpaEntity> findByTimelineIdOrderByEventTimestampAsc(UUID timelineId);
    
    /**
     * Find events by timeline ID and event type
     * @param timelineId Timeline ID
     * @param eventType Type of event
     * @return List of matching events
     */
    List<DomainTripEventJpaEntity> findByTimelineIdAndEventType(UUID timelineId, String eventType);
    
    /**
     * Find events after a specific timestamp for a timeline
     * @param timelineId Timeline ID
     * @param timestamp Timestamp to filter from
     * @return List of events after the timestamp
     */
    @Query("SELECT e FROM DomainTripEventJpaEntity e WHERE e.timelineId = :timelineId AND e.eventTimestamp > :timestamp ORDER BY e.eventTimestamp ASC")
    List<DomainTripEventJpaEntity> findEventsSince(
        @Param("timelineId") UUID timelineId,
        @Param("timestamp") LocalDateTime timestamp
    );
}
