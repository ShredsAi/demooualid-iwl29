package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.events.DomainDomainEvent;
import ai.shreds.domain.ports.DomainEventStoreOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureRepositoryException;
import ai.shreds.infrastructure.mappers.InfrastructureEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InfrastructureEventStoreRepositoryImpl implements DomainEventStoreOutputPort {

    private final InfrastructureOutboxEventJpaRepository outboxEventJpaRepository;
    private final InfrastructureEventMapper eventMapper;

    @Override
    public void saveEvent(DomainDomainEvent event) {
        try {
            log.debug("Saving domain event to outbox: type={}, aggregateId={}", 
                    event.getEventType(), event.getAggregateId());

            var outboxEntity = eventMapper.toOutboxEntity(event);
            outboxEventJpaRepository.save(outboxEntity);

            log.debug("Successfully saved domain event to outbox: type={}, aggregateId={}", 
                    event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to save domain event to outbox: type={}, aggregateId={}", 
                    event.getEventType(), event.getAggregateId(), e);
            throw new InfrastructureRepositoryException("saveEvent", "OutboxEvent", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainDomainEvent> getUnpublishedEvents() {
        try {
            log.debug("Retrieving unpublished events from outbox");

            List<DomainDomainEvent> events = outboxEventJpaRepository.findByPublishedFalse()
                    .stream()
                    .map(eventMapper::fromOutboxEntity)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} unpublished events from outbox", events.size());
            return events;
        } catch (Exception e) {
            log.error("Failed to retrieve unpublished events from outbox", e);
            throw new InfrastructureRepositoryException("getUnpublishedEvents", "OutboxEvent", e);
        }
    }

    @Override
    public void markAsPublished(long eventId) {
        try {
            log.debug("Marking outbox event as published: eventId={}", eventId);

            outboxEventJpaRepository.updatePublished(eventId, true, OffsetDateTime.now());

            log.debug("Successfully marked outbox event as published: eventId={}", eventId);
        } catch (Exception e) {
            log.error("Failed to mark outbox event as published: eventId={}", eventId, e);
            throw new InfrastructureRepositoryException("markAsPublished", "OutboxEvent", e);
        }
    }

    public List<DomainDomainEvent> getUnpublishedEventsWithLimit(int limit) {
        try {
            log.debug("Retrieving {} unpublished events from outbox", limit);

            List<DomainDomainEvent> events = outboxEventJpaRepository.findUnpublishedEventsWithLimit(limit)
                    .stream()
                    .map(eventMapper::fromOutboxEntity)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} unpublished events from outbox", events.size());
            return events;
        } catch (Exception e) {
            log.error("Failed to retrieve unpublished events with limit from outbox", e);
            throw new InfrastructureRepositoryException("getUnpublishedEventsWithLimit", "OutboxEvent", e);
        }
    }

    public long countUnpublishedEvents() {
        try {
            long count = outboxEventJpaRepository.countUnpublishedEvents();
            log.debug("Count of unpublished events: {}", count);
            return count;
        } catch (Exception e) {
            log.error("Failed to count unpublished events", e);
            throw new InfrastructureRepositoryException("countUnpublishedEvents", "OutboxEvent", e);
        }
    }
}
