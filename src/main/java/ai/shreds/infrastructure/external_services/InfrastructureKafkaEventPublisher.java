package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureMessagingException;
import ai.shreds.shared.dtos.SharedTripEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class InfrastructureKafkaEventPublisher implements ApplicationEventPublisherOutputPort {

    private final KafkaTemplate<String, SharedTripEventDTO> kafkaTemplate;
    private final String tripEventsTopic;
    private final ConcurrentMap<String, String> publishedEvents = new ConcurrentHashMap<>();

    public InfrastructureKafkaEventPublisher(
            KafkaTemplate<String, SharedTripEventDTO> kafkaTemplate,
            @Value("${kafka.topics.trip-lifecycle-events}") String tripEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tripEventsTopic = tripEventsTopic;
    }

    @Override
    public void publishTripEvent(SharedTripEventDTO event) {
        log.debug("Publishing trip event: type={}, tripId={}", event.getEventType(), event.getTripId());

        try {
            ensureIdempotency(event);
            
            kafkaTemplate.send(tripEventsTopic, event.getTripId(), event)
                    .thenApply(result -> {
                        log.debug("Successfully published trip event: type={}, tripId={}, partition={}, offset={}",
                                event.getEventType(),
                                event.getTripId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                        return result;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish trip event: type={}, tripId={}, error={}",
                                event.getEventType(), event.getTripId(), ex.getMessage(), ex);
                        removeFromIdempotencyCache(event);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error publishing trip event: type={}, tripId={}, error={}", 
                    event.getEventType(), event.getTripId(), e.getMessage(), e);
            removeFromIdempotencyCache(event);
            throw new InfrastructureMessagingException(tripEventsTopic, event.getTripId(), e);
        }
    }

    private void ensureIdempotency(SharedTripEventDTO event) {
        String eventKey = generateEventKey(event);
        String existingTimestamp = publishedEvents.putIfAbsent(eventKey, event.getTimestamp());
        if (existingTimestamp != null) {
            log.debug("Event already published, skipping: type={}, tripId={}", 
                    event.getEventType(), event.getTripId());
            return;
        }
        log.debug("Event marked for publishing: type={}, tripId={}", 
                event.getEventType(), event.getTripId());
    }

    private void removeFromIdempotencyCache(SharedTripEventDTO event) {
        String eventKey = generateEventKey(event);
        publishedEvents.remove(eventKey);
    }

    private String generateEventKey(SharedTripEventDTO event) {
        return event.getTripId() + ":" + event.getEventType() + ":" + event.getTimestamp();
    }

    public void cleanupIdempotencyCache() {
        if (publishedEvents.size() > 10000) {
            log.info("Clearing idempotency cache, current size: {}", publishedEvents.size());
            publishedEvents.clear();
        }
    }
}