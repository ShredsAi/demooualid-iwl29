package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationDriverAssignedCommand;
import ai.shreds.application.dtos.ApplicationMatchingTimeoutCommand;
import ai.shreds.application.exceptions.ApplicationServiceException;
import ai.shreds.application.ports.ApplicationDriverServiceOutputPort;
import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationMatchingCoordinatorInputPort;
import ai.shreds.application.ports.ApplicationTripExecutionNotificationOutputPort;
import ai.shreds.application.ports.ApplicationTripRepositoryOutputPort;
import ai.shreds.domain.commands.DomainAssignDriverCommand;
import ai.shreds.domain.commands.DomainMatchingTimeoutCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.ports.DomainTripServiceInputPort;
import ai.shreds.shared.dtos.SharedDriverProfileDTO;
import ai.shreds.shared.dtos.SharedTripEventDTO;
import ai.shreds.shared.dtos.SharedTripMatchedMessageDTO;
import ai.shreds.shared.exceptions.SharedNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationMatchingCoordinatorService implements ApplicationMatchingCoordinatorInputPort {

    private final ApplicationTripRepositoryOutputPort tripRepository;
    private final ApplicationDriverServiceOutputPort driverService;
    private final ApplicationEventPublisherOutputPort eventPublisher;
    private final ApplicationTripExecutionNotificationOutputPort tripExecutionNotification;
    private final DomainTripServiceInputPort domainTripService;
    private final ApplicationTimeoutSchedulerService timeoutScheduler;

    @Override
    @Transactional
    public void handleDriverAssigned(ApplicationDriverAssignedCommand command) {
        log.info("Handling driver assigned for trip: {}, driver: {}", 
            command.getTripId(), command.getDriverId());
        
        try {
            // Step 1: Retrieve trip entity
            DomainTripEntity trip = tripRepository.findById(command.getTripId())
                .orElseThrow(() -> new SharedNotFoundException("Trip", command.getTripId()));
            
            // Step 2: Validate driver availability and enrich command with driver details
            SharedDriverProfileDTO driverProfile = validateDriverAvailability(command.getDriverId());
            enrichCommandWithDriverDetails(command, driverProfile);
            
            // Step 3: Assign driver through domain service
            DomainAssignDriverCommand domainCommand = command.toDomainCommand();
            DomainTripEntity updatedTrip = domainTripService.assignDriver(domainCommand);
            
            // Step 4: Cancel any pending timeout
            timeoutScheduler.cancelTimeout(command.getTripId());
            
            // Step 5: Notify Trip Execution Shred
            notifyTripExecutionShred(updatedTrip);
            
            // Step 6: Publish driver matched event
            publishDriverMatchedEvent(updatedTrip);
            
            log.info("Successfully handled driver assignment for trip: {}", command.getTripId());
            
        } catch (SharedNotFoundException e) {
            throw e; // Re-throw shared exceptions
        } catch (Exception e) {
            log.error("Error handling driver assigned for trip: {}", command.getTripId(), e);
            throw new ApplicationServiceException("Failed to handle driver assignment", e);
        }
    }

    @Override
    @Transactional
    public void handleMatchingTimeout(ApplicationMatchingTimeoutCommand command) {
        log.info("Handling matching timeout for trip: {}, reason: {}", 
            command.getTripId(), command.getReason());
        
        try {
            // Step 1: Process timeout through domain service
            DomainMatchingTimeoutCommand domainCommand = command.toDomainCommand();
            DomainTripEntity timeoutTrip = domainTripService.handleMatchingTimeout(domainCommand);
            
            // Step 2: Publish timeout event
            publishTimeoutEvent(timeoutTrip, command.getReason());
            
            log.info("Successfully handled matching timeout for trip: {}", command.getTripId());
            
        } catch (SharedNotFoundException e) {
            throw e; // Re-throw shared exceptions
        } catch (Exception e) {
            log.error("Error handling matching timeout for trip: {}", command.getTripId(), e);
            throw new ApplicationServiceException("Failed to handle matching timeout", e);
        }
    }
    
    private SharedDriverProfileDTO validateDriverAvailability(String driverId) {
        try {
            SharedDriverProfileDTO driverProfile = driverService.validateDriver(driverId);
            if (!driverProfile.isAvailable()) {
                throw new ApplicationServiceException(
                    "Driver " + driverId + " is not available for assignment"
                );
            }
            log.debug("Driver {} validation successful", driverId);
            return driverProfile;
        } catch (Exception e) {
            if (e instanceof ApplicationServiceException) {
                throw e;
            }
            throw new ApplicationServiceException("Failed to validate driver: " + driverId, e);
        }
    }
    
    private void enrichCommandWithDriverDetails(ApplicationDriverAssignedCommand command, SharedDriverProfileDTO driverProfile) {
        // Enrich command with driver details from profile
        // In a real scenario, you might have more driver details in the profile
        if (command.getDriverName() == null) {
            command.setDriverName("Driver " + command.getDriverId().substring(0, 8));
        }
        if (command.getDriverPhone() == null) {
            command.setDriverPhone("+1234567890"); // Should come from driver service
        }
        if (command.getDriverRating() == null) {
            command.setDriverRating(driverProfile.getRating());
        }
    }
    
    private void notifyTripExecutionShred(DomainTripEntity trip) {
        try {
            SharedTripMatchedMessageDTO message = new SharedTripMatchedMessageDTO();
            message.setTripId(trip.getId().getValue());
            message.setDriverId(trip.getDriver().getId());
            message.setTripStatus("MATCHED");
            message.setMatchedAt(LocalDateTime.now().toString());
            
            tripExecutionNotification.notifyTripMatched(message);
            
            log.info("Notified Trip Execution Shred for trip: {}", trip.getId().getValue());
        } catch (Exception e) {
            log.error("Failed to notify Trip Execution Shred for trip: {}", 
                trip.getId().getValue(), e);
            // Don't rethrow - this is a non-critical operation
        }
    }
    
    private void publishDriverMatchedEvent(DomainTripEntity trip) {
        SharedTripEventDTO event = new SharedTripEventDTO();
        event.setEventType("DRIVER_MATCHED");
        event.setTripId(trip.getId().getValue());
        event.setTimestamp(LocalDateTime.now().toString());
        event.setSource("trip-request-matching-shred");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("tripId", trip.getId().getValue());
        payload.put("riderId", trip.getRider().getId());
        payload.put("driverId", trip.getDriver().getId());
        payload.put("driverName", trip.getDriver().getName());
        payload.put("driverPhone", trip.getDriver().getMaskedPhone());
        payload.put("driverRating", trip.getDriver().getRating());
        payload.put("matchedAt", LocalDateTime.now().toString());
        payload.put("pickupLocation", trip.getPickupLocation().toSharedDTO());
        payload.put("dropoffLocation", trip.getDropoffLocation().toSharedDTO());
        event.setPayload(payload);
        
        eventPublisher.publishTripEvent(event);
        
        log.info("Published DRIVER_MATCHED event for trip: {}", trip.getId().getValue());
    }
    
    private void publishTimeoutEvent(DomainTripEntity trip, String reason) {
        SharedTripEventDTO event = new SharedTripEventDTO();
        event.setEventType("MATCH_TIMEOUT");
        event.setTripId(trip.getId().getValue());
        event.setTimestamp(LocalDateTime.now().toString());
        event.setSource("trip-request-matching-shred");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("tripId", trip.getId().getValue());
        payload.put("riderId", trip.getRider().getId());
        payload.put("reason", reason);
        payload.put("timeoutAt", LocalDateTime.now().toString());
        payload.put("originalStatus", DomainTripStatusEnum.MATCHING.name());
        payload.put("newStatus", DomainTripStatusEnum.MATCH_FAILED.name());
        event.setPayload(payload);
        
        eventPublisher.publishTripEvent(event);
        
        log.info("Published MATCH_TIMEOUT event for trip: {}", trip.getId().getValue());
    }
}