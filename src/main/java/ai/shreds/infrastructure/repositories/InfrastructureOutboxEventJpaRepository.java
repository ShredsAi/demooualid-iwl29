package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainOutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface InfrastructureOutboxEventJpaRepository extends JpaRepository<DomainOutboxEventJpaEntity, Long> {

    /**
     * Save outbox event
     * @param entity Outbox event entity
     * @return Saved entity
     */
    DomainOutboxEventJpaEntity save(DomainOutboxEventJpaEntity entity);

    /**
     * Find all unpublished events
     * @return List of unpublished events
     */
    @Query("SELECT e FROM DomainOutboxEventJpaEntity e WHERE e.published = false ORDER BY e.createdAt ASC")
    List<DomainOutboxEventJpaEntity> findByPublishedFalse();

    /**
     * Find unpublished events with limit
     * @return List of unpublished events with limit
     */
    @Query(value = "SELECT * FROM outbox_event WHERE published = false ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DomainOutboxEventJpaEntity> findUnpublishedEventsWithLimit(@Param("limit") int limit);

    /**
     * Update published status
     * @param outboxId Outbox event ID
     * @param published Published status
     * @param publishedAt Published timestamp
     */
    @Modifying
    @Query("UPDATE DomainOutboxEventJpaEntity e SET e.published = :published, e.publishedAt = :publishedAt WHERE e.outboxId = :outboxId")
    void updatePublished(@Param("outboxId") Long outboxId, @Param("published") Boolean published, @Param("publishedAt") OffsetDateTime publishedAt);

    /**
     * Find events by aggregate type and ID
     * @param aggregateType Aggregate type
     * @param aggregateId Aggregate ID
     * @return List of events for the aggregate
     */
    @Query("SELECT e FROM DomainOutboxEventJpaEntity e WHERE e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId ORDER BY e.createdAt ASC")
    List<DomainOutboxEventJpaEntity> findByAggregateTypeAndAggregateId(@Param("aggregateType") String aggregateType, @Param("aggregateId") java.util.UUID aggregateId);

    /**
     * Count unpublished events
     * @return Count of unpublished events
     */
    @Query("SELECT COUNT(e) FROM DomainOutboxEventJpaEntity e WHERE e.published = false")
    long countUnpublishedEvents();

    /**
     * Delete old published events
     * @param cutoffDate Events older than this date
     */
    @Modifying
    @Query("DELETE FROM DomainOutboxEventJpaEntity e WHERE e.published = true AND e.publishedAt < :cutoffDate")
    void deletePublishedEventsBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);
}