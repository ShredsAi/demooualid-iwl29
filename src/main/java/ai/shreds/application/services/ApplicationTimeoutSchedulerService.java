package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationTimeoutCommand;
import ai.shreds.application.exceptions.ApplicationServiceException;
import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationTimeoutInputPort;
import ai.shreds.application.ports.ApplicationTripRepositoryOutputPort;
import ai.shreds.domain.commands.DomainTimeoutCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.ports.DomainTripServiceInputPort;
import ai.shreds.shared.dtos.SharedTripEventDTO;
import ai.shreds.shared.exceptions.SharedNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationTimeoutSchedulerService implements ApplicationTimeoutInputPort {

    private final ApplicationTripRepositoryOutputPort tripRepository;
    private final ApplicationEventPublisherOutputPort eventPublisher;
    private final DomainTripServiceInputPort domainTripService;
    private final TaskScheduler taskScheduler;
    private final MessageChannel timeoutChannel;
    
    @Value("${matching.timeout.duration:300000}") // 5 minutes default
    private long defaultTimeoutDuration;
    
    // Store scheduled futures to allow cancellation
    private final Map<String, ScheduledFuture<?>> scheduledTimeouts = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void handleTimeout(ApplicationTimeoutCommand command) {
        log.info("Handling internal timeout for trip: {}", command.getTripId());
        
        try {
            // Step 1: Retrieve trip entity
            DomainTripEntity trip = tripRepository.findById(command.getTripId())
                .orElseThrow(() -> new SharedNotFoundException("Trip", command.getTripId()));
            
            // Step 2: Check if trip is still in a state that can timeout
            if (trip.getStatus() != DomainTripStatusEnum.MATCHING) {
                log.info("Trip {} is no longer in MATCHING state, ignoring timeout", 
                    command.getTripId());
                return;
            }
            
            // Step 3: Process timeout through domain service
            DomainTimeoutCommand domainCommand = command.toDomainCommand();
            DomainTripEntity timeoutTrip = domainTripService.handleInternalTimeout(domainCommand);
            
            // Step 4: Remove from scheduled timeouts map
            scheduledTimeouts.remove(command.getTripId());
            
            // Step 5: Publish timeout event
            publishTimeoutEvent(timeoutTrip);
            
            log.info("Successfully handled internal timeout for trip: {}", command.getTripId());
            
        } catch (SharedNotFoundException e) {
            log.warn("Trip not found for timeout: {}", command.getTripId());
            scheduledTimeouts.remove(command.getTripId());
        } catch (Exception e) {
            log.error("Error handling internal timeout for trip: {}", command.getTripId(), e);
            scheduledTimeouts.remove(command.getTripId());
            throw new ApplicationServiceException("Failed to handle internal timeout", e);
        }
    }

    public void scheduleTimeout(String tripId, long duration) {
        log.info("Scheduling timeout for trip: {}, duration: {}ms", tripId, duration);
        
        try {
            // Cancel any existing timeout for this trip
            cancelTimeout(tripId);
            
            // Create timeout command
            ApplicationTimeoutCommand timeoutCommand = new ApplicationTimeoutCommand();
            timeoutCommand.setTripId(tripId);
            timeoutCommand.setTimeoutAt(LocalDateTime.now().plus(Duration.ofMillis(duration)).toString());
            timeoutCommand.setDuration(duration);
            
            // Schedule the timeout
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                () -> handleTimeoutAsync(timeoutCommand),
                Instant.now().plusMillis(duration)
            );
            
            // Store the scheduled future for potential cancellation
            scheduledTimeouts.put(tripId, scheduledFuture);
            
            log.info("Successfully scheduled timeout for trip: {}", tripId);
            
        } catch (Exception e) {
            log.error("Error scheduling timeout for trip: {}", tripId, e);
            throw new ApplicationServiceException("Failed to schedule timeout", e);
        }
    }

    public void cancelTimeout(String tripId) {
        log.debug("Attempting to cancel timeout for trip: {}", tripId);
        
        ScheduledFuture<?> scheduledFuture = scheduledTimeouts.remove(tripId);
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            boolean cancelled = scheduledFuture.cancel(false);
            log.info("Timeout cancellation for trip: {}, success: {}", tripId, cancelled);
        } else {
            log.debug("No active timeout found for trip: {}", tripId);
        }
    }
    
    /**
     * Schedule timeout with default duration
     */
    public void scheduleTimeout(String tripId) {
        scheduleTimeout(tripId, defaultTimeoutDuration);
    }
    
    /**
     * Get count of active scheduled timeouts (for monitoring)
     */
    public int getActiveTimeoutCount() {
        // Clean up completed futures
        scheduledTimeouts.entrySet().removeIf(entry -> entry.getValue().isDone());
        return scheduledTimeouts.size();
    }
    
    private void handleTimeoutAsync(ApplicationTimeoutCommand command) {
        try {
            // Send timeout message through Spring Integration channel for async processing
            timeoutChannel.send(
                MessageBuilder.withPayload(command)
                    .setHeader("eventType", "MATCHING_TIMER_EXPIRED")
                    .setHeader("tripId", command.getTripId())
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending timeout message for trip: {}", command.getTripId(), e);
            // Fallback to direct handling
            try {
                handleTimeout(command);
            } catch (Exception fallbackError) {
                log.error("Fallback timeout handling also failed for trip: {}", 
                    command.getTripId(), fallbackError);
            }
        }
    }
    
    private void publishTimeoutEvent(DomainTripEntity trip) {
        SharedTripEventDTO event = new SharedTripEventDTO();
        event.setEventType("MATCH_TIMEOUT");
        event.setTripId(trip.getId().getValue());
        event.setTimestamp(LocalDateTime.now().toString());
        event.setSource("trip-request-matching-shred-internal-timer");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("tripId", trip.getId().getValue());
        payload.put("riderId", trip.getRider().getId());
        payload.put("reason", "INTERNAL_TIMEOUT");
        payload.put("timeoutAt", LocalDateTime.now().toString());
        payload.put("timeoutDuration", defaultTimeoutDuration);
        payload.put("originalStatus", DomainTripStatusEnum.MATCHING.name());
        payload.put("newStatus", DomainTripStatusEnum.MATCH_FAILED.name());
        event.setPayload(payload);
        
        eventPublisher.publishTripEvent(event);
        
        log.info("Published internal timeout event for trip: {}", trip.getId().getValue());
    }
}