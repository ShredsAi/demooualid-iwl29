package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedTripEventDTO;

/**
 * Output port for event publishing operations.
 * This port defines the contract for publishing trip lifecycle events to Kafka.
 */
public interface ApplicationEventPublisherOutputPort {
    
    /**
     * Publishes a trip lifecycle event to the event bus (Kafka).
     * 
     * @param event The trip event to publish
     * @throws ai.shreds.infrastructure.exceptions.InfrastructureMessagingException if publishing fails
     */
    void publishTripEvent(SharedTripEventDTO event);
}