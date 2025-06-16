package ai.shreds.adapters.primary;

import ai.shreds.shared.dtos.SharedTimerExpiredEventDTO;
import ai.shreds.application.ports.ApplicationTimeoutInputPort;
import ai.shreds.application.dtos.ApplicationTimeoutCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdapterTimerEventHandler {

    private final ApplicationTimeoutInputPort applicationTimeoutPort;

    @ServiceActivator(inputChannel = "matchingTimerChannel")
    public void handleMatchingTimerExpired(
            @Payload SharedTimerExpiredEventDTO event,
            @Header(value = "tripId", required = false) String tripIdHeader) {
        
        String tripId = event.getTripId() != null ? event.getTripId() : tripIdHeader;
        log.info("Processing matching timer expired event for trip: {}, timeout at: {}", 
                tripId, event.getTimeoutAt());
        
        try {
            ApplicationTimeoutCommand command = mapToTimeoutCommand(event);
            applicationTimeoutPort.handleTimeout(command);
            
            log.info("Successfully processed matching timer expiration for trip: {}", tripId);
        } catch (Exception e) {
            log.error("Failed to process matching timer expired event for trip: {}", tripId, e);
            throw new RuntimeException("Failed to handle matching timer expiration", e);
        }
    }

    @ServiceActivator(inputChannel = "internalTimerChannel")
    public void handleInternalTimerEvent(
            @Payload Object timerEvent,
            @Header(value = "eventType", required = false) String eventType,
            @Header(value = "tripId", required = false) String tripId) {
        
        log.info("Processing internal timer event of type: {} for trip: {}", eventType, tripId);
        
        try {
            if ("MATCHING_TIMER_EXPIRED".equals(eventType)) {
                if (timerEvent instanceof SharedTimerExpiredEventDTO) {
                    handleMatchingTimerExpired((SharedTimerExpiredEventDTO) timerEvent, tripId);
                } else {
                    // Create event from headers if payload is not the expected type
                    SharedTimerExpiredEventDTO event = createTimerEventFromHeaders(eventType, tripId);
                    handleMatchingTimerExpired(event, tripId);
                }
            } else {
                log.warn("Unknown internal timer event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process internal timer event for trip: {}", tripId, e);
            throw new RuntimeException("Failed to handle internal timer event", e);
        }
    }

    public void handleTimerExpiredEvent(SharedTimerExpiredEventDTO event) {
        log.info("Processing timer expired event for trip: {}", event.getTripId());
        
        try {
            ApplicationTimeoutCommand command = mapToTimeoutCommand(event);
            applicationTimeoutPort.handleTimeout(command);
            
            log.info("Successfully processed timer expiration for trip: {}", event.getTripId());
        } catch (Exception e) {
            log.error("Failed to process timer expired event for trip: {}", event.getTripId(), e);
            throw new RuntimeException("Failed to handle timer expiration", e);
        }
    }

    private ApplicationTimeoutCommand mapToTimeoutCommand(SharedTimerExpiredEventDTO event) {
        return event.toApplicationCommand();
    }

    private SharedTimerExpiredEventDTO createTimerEventFromHeaders(String eventType, String tripId) {
        return SharedTimerExpiredEventDTO.builder()
                .eventType(eventType)
                .tripId(tripId)
                .timeoutAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .duration(300000L) // Default 5 minutes in milliseconds
                .build();
    }

    // Helper method for manual timer triggering in case of programmatic timeout
    public void triggerManualTimeout(String tripId, Long durationMs) {
        log.info("Manually triggering timeout for trip: {} after {} ms", tripId, durationMs);
        
        SharedTimerExpiredEventDTO event = SharedTimerExpiredEventDTO.builder()
                .eventType("MATCHING_TIMER_EXPIRED")
                .tripId(tripId)
                .timeoutAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .duration(durationMs)
                .build();
        
        handleTimerExpiredEvent(event);
    }

    // Method to handle timeout based on trip ID for integration with scheduling services
    public void handleScheduledTimeout(String tripId, LocalDateTime scheduledTime, Long duration) {
        log.info("Handling scheduled timeout for trip: {} at: {}", tripId, scheduledTime);
        
        SharedTimerExpiredEventDTO event = SharedTimerExpiredEventDTO.builder()
                .eventType("MATCHING_TIMER_EXPIRED")
                .tripId(tripId)
                .timeoutAt(scheduledTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .duration(duration)
                .build();
        
        try {
            ApplicationTimeoutCommand command = mapToTimeoutCommand(event);
            applicationTimeoutPort.handleTimeout(command);
            
            log.info("Successfully processed scheduled timeout for trip: {}", tripId);
        } catch (Exception e) {
            log.error("Failed to process scheduled timeout for trip: {}", tripId, e);
            throw new RuntimeException("Failed to handle scheduled timeout", e);
        }
    }
}