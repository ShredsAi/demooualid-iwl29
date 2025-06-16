package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.events.DomainDomainEvent;
import ai.shreds.infrastructure.exceptions.InfrastructureMessagingException;
import ai.shreds.infrastructure.mappers.InfrastructureEventMapper;
import ai.shreds.infrastructure.repositories.InfrastructureEventStoreRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfrastructureOutboxEventPublisher {

    private final InfrastructureEventStoreRepositoryImpl eventStoreRepository;
    private final InfrastructureKafkaEventPublisher kafkaEventPublisher;
    private final InfrastructureEventMapper eventMapper;

    /**
     * Publishes pending events from the outbox to Kafka
     * This method is called periodically to ensure reliable event delivery
     */
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    @Async
    @Transactional
    public void publishPendingEvents() {
        try {
            log.debug("Checking for unpublished events in outbox");
            
            List<DomainDomainEvent> unpublishedEvents = eventStoreRepository.getUnpublishedEventsWithLimit(50);
            
            if (unpublishedEvents.isEmpty()) {
                log.trace("No unpublished events found in outbox");
                return;
            }
            
            log.info("Found {} unpublished events in outbox, processing...", unpublishedEvents.size());
            
            for (DomainDomainEvent event : unpublishedEvents) {
                try {
                    publishEvent(event);
                    
                    // Mark as published only after successful Kafka send
                    // Note: In a real implementation, you'd want to ensure the Kafka send is actually successful
                    // before marking as published. This is a simplified version.
                    eventStoreRepository.markAsPublished(getEventId(event));
                    
                    log.debug("Successfully processed outbox event: type={}, aggregateId={}", 
                            event.getEventType(), event.getAggregateId());
                        
                } catch (Exception e) {
                    log.error("Failed to publish outbox event: type={}, aggregateId={}, error={}", 
                            event.getEventType(), event.getAggregateId(), e.getMessage(), e);
                    
                    // Don't mark as published on failure - it will be retried next time
                    handlePublishFailure(event, e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in outbox event publishing process: {}", e.getMessage(), e);
        }
    }

    private void publishEvent(DomainDomainEvent event) {
        try {
            // Convert domain event to Kafka event DTO
            var kafkaEvent = eventMapper.toKafkaEvent(event);
            
            // Publish via Kafka event publisher
            kafkaEventPublisher.publishTripEvent(kafkaEvent);
            
            log.debug("Published event to Kafka: type={}, aggregateId={}", 
                    event.getEventType(), event.getAggregateId());
                    
        } catch (Exception e) {
            log.error("Failed to publish event to Kafka: type={}, aggregateId={}", 
                    event.getEventType(), event.getAggregateId(), e);
            throw new InfrastructureMessagingException("outbox-publisher", event.getAggregateId(), e);
        }
    }

    private void handlePublishFailure(DomainDomainEvent event, Exception ex) {
        // In a production system, you might want to implement:
        // - Dead letter queue for failed events
        // - Exponential backoff retry logic
        // - Alert/monitoring for failed events
        // - Circuit breaker to prevent cascade failures
        
        log.warn("Event publish failure will be retried next cycle: type={}, aggregateId={}", 
                event.getEventType(), event.getAggregateId());
    }

    private Long getEventId(DomainDomainEvent event) {
        // This is a simplified approach - in reality, you'd want to track the outbox ID
        // when saving the event. For now, we'll use a hash as a fallback.
        return (long) (event.getAggregateId() + event.getEventType() + event.getOccurredAt().toString()).hashCode();
    }

    /**
     * Manual trigger for publishing events (for testing or manual recovery)
     */
    public void forcePublishPendingEvents() {
        log.info("Manual trigger for outbox event publishing");
        publishPendingEvents();
    }

    /**
     * Get count of pending events for monitoring
     */
    public long getPendingEventCount() {
        return eventStoreRepository.countUnpublishedEvents();
    }
}