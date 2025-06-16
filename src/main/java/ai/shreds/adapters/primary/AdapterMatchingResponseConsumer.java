package ai.shreds.adapters.primary;

import ai.shreds.shared.dtos.SharedDriverAssignedEventDTO;
import ai.shreds.shared.dtos.SharedMatchingTimeoutEventDTO;
import ai.shreds.application.ports.ApplicationMatchingCoordinatorInputPort;
import ai.shreds.application.dtos.ApplicationDriverAssignedCommand;
import ai.shreds.application.dtos.ApplicationMatchingTimeoutCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdapterMatchingResponseConsumer {

    private final ApplicationMatchingCoordinatorInputPort applicationMatchingCoordinatorPort;

    @KafkaListener(
        topics = "#{@kafkaTopicConfig.getMatchingServiceResponsesTopic()}",
        groupId = "trip-request-matching-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMatchingResponse(
            @Payload String payload,
            @Header("kafka_receivedMessageKey") String messageKey,
            @Header(value = "eventType", required = false) String eventType,
            @Header(value = "correlationId", required = false) String correlationId,
            Acknowledgment acknowledgment) {
        
        log.info("Received matching response with eventType: {}, correlationId: {}, key: {}", 
                eventType, correlationId, messageKey);
        
        try {
            if ("DRIVER_ASSIGNED".equals(eventType)) {
                handleDriverAssignedEvent(payload, correlationId);
            } else if ("MATCHING_TIMEOUT".equals(eventType)) {
                handleMatchingTimeoutEvent(payload, correlationId);
            } else {
                log.warn("Unknown event type received: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            log.debug("Message processed and acknowledged successfully");
            
        } catch (Exception e) {
            log.error("Error processing matching response with eventType: {}, correlationId: {}", 
                    eventType, correlationId, e);
            // Don't acknowledge on error - message will be retried
            throw new RuntimeException("Failed to process matching response", e);
        }
    }

    public void handleDriverAssigned(SharedDriverAssignedEventDTO event) {
        log.info("Processing driver assigned event for trip: {}, driver: {}", 
                event.getTripId(), event.getDriverId());
        
        try {
            ApplicationDriverAssignedCommand command = mapToDriverAssignedCommand(event);
            applicationMatchingCoordinatorPort.handleDriverAssigned(command);
            
            log.info("Successfully processed driver assignment for trip: {}", event.getTripId());
        } catch (Exception e) {
            log.error("Failed to process driver assigned event for trip: {}", event.getTripId(), e);
            throw new RuntimeException("Failed to handle driver assignment", e);
        }
    }

    public void handleMatchingTimeout(SharedMatchingTimeoutEventDTO event) {
        log.info("Processing matching timeout event for trip: {}, reason: {}", 
                event.getTripId(), event.getReason());
        
        try {
            ApplicationMatchingTimeoutCommand command = mapToMatchingTimeoutCommand(event);
            applicationMatchingCoordinatorPort.handleMatchingTimeout(command);
            
            log.info("Successfully processed matching timeout for trip: {}", event.getTripId());
        } catch (Exception e) {
            log.error("Failed to process matching timeout event for trip: {}", event.getTripId(), e);
            throw new RuntimeException("Failed to handle matching timeout", e);
        }
    }

    private void handleDriverAssignedEvent(String payload, String correlationId) {
        try {
            SharedDriverAssignedEventDTO event = parseDriverAssignedEvent(payload);
            event.setCorrelationId(correlationId);
            handleDriverAssigned(event);
        } catch (Exception e) {
            log.error("Failed to parse driver assigned event from payload: {}", payload, e);
            throw new RuntimeException("Failed to parse driver assigned event", e);
        }
    }

    private void handleMatchingTimeoutEvent(String payload, String correlationId) {
        try {
            SharedMatchingTimeoutEventDTO event = parseMatchingTimeoutEvent(payload);
            event.setCorrelationId(correlationId);
            handleMatchingTimeout(event);
        } catch (Exception e) {
            log.error("Failed to parse matching timeout event from payload: {}", payload, e);
            throw new RuntimeException("Failed to parse matching timeout event", e);
        }
    }

    private SharedDriverAssignedEventDTO parseDriverAssignedEvent(String payload) {
        try {
            // Using Jackson ObjectMapper for JSON parsing
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, SharedDriverAssignedEventDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse driver assigned event JSON: {}", payload, e);
            throw new RuntimeException("JSON parsing failed", e);
        }
    }

    private SharedMatchingTimeoutEventDTO parseMatchingTimeoutEvent(String payload) {
        try {
            // Using Jackson ObjectMapper for JSON parsing
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, SharedMatchingTimeoutEventDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse matching timeout event JSON: {}", payload, e);
            throw new RuntimeException("JSON parsing failed", e);
        }
    }

    private ApplicationDriverAssignedCommand mapToDriverAssignedCommand(SharedDriverAssignedEventDTO event) {
        return event.toApplicationCommand();
    }

    private ApplicationMatchingTimeoutCommand mapToMatchingTimeoutCommand(SharedMatchingTimeoutEventDTO event) {
        return event.toApplicationCommand();
    }
}
