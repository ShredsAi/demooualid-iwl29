package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationCancelTripCommand;
import ai.shreds.application.dtos.ApplicationCancellationResultDTO;
import ai.shreds.application.exceptions.ApplicationServiceException;
import ai.shreds.application.ports.ApplicationCancelTripInputPort;
import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.application.ports.ApplicationTripRepositoryOutputPort;
import ai.shreds.domain.commands.DomainCancelTripCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.ports.DomainTripServiceInputPort;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.dtos.SharedTripEventDTO;
import ai.shreds.shared.exceptions.SharedConflictException;
import ai.shreds.shared.exceptions.SharedNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationCancellationService implements ApplicationCancelTripInputPort {

    private final ApplicationTripRepositoryOutputPort tripRepository;
    private final ApplicationPaymentServiceOutputPort paymentService;
    private final ApplicationEventPublisherOutputPort eventPublisher;
    private final DomainTripServiceInputPort domainTripService;

    @Override
    @Transactional
    public ApplicationCancellationResultDTO cancelTrip(ApplicationCancelTripCommand command) {
        log.info("Cancelling trip: {}, reason: {}", command.getTripId(), command.getReason());
        
        try {
            // Step 1: Retrieve trip entity
            DomainTripEntity trip = tripRepository.findById(command.getTripId())
                .orElseThrow(() -> new SharedNotFoundException("Trip", command.getTripId()));
            
            // Step 2: Validate trip can be cancelled
            validateTripCanBeCancelled(trip);
            
            // Step 3: Process refund if needed
            SharedMoneyDTO refundAmount = processRefund(trip);
            
            // Step 4: Create domain command with refund amount
            DomainMoneyValue domainRefundAmount = refundAmount != null ? 
                DomainMoneyValue.fromSharedDTO(refundAmount) : 
                null;
            
            DomainCancelTripCommand domainCommand = command.toDomainCommand(domainRefundAmount);
            
            // Step 5: Cancel trip through domain service
            DomainTripEntity cancelledTrip = domainTripService.cancelTrip(domainCommand);
            
            // Step 6: Publish cancellation event
            publishCancellationEvent(cancelledTrip, command.getReason());
            
            // Step 7: Create and return result
            return ApplicationCancellationResultDTO.fromDomainEntity(
                cancelledTrip, 
                refundAmount != null ? refundAmount : createZeroAmount()
            );
            
        } catch (SharedNotFoundException | SharedConflictException e) {
            throw e; // Re-throw shared exceptions
        } catch (Exception e) {
            log.error("Error cancelling trip: {}", command.getTripId(), e);
            throw new ApplicationServiceException("Failed to cancel trip", e);
        }
    }
    
    private void validateTripCanBeCancelled(DomainTripEntity trip) {
        if (!trip.canBeCancelled()) {
            throw new SharedConflictException(
                "Trip cannot be cancelled in current state",
                "INVALID_STATE_FOR_CANCELLATION",
                trip.getStatus().name()
            );
        }
    }
    
    private SharedMoneyDTO processRefund(DomainTripEntity trip) {
        // Check if trip is in a state where refund is applicable
        if (trip.getStatus() == DomainTripStatusEnum.REQUESTED || 
            trip.getStatus() == DomainTripStatusEnum.MATCHING) {
            
            try {
                // Attempt to refund the pre-authorized amount
                // Note: In a real implementation, we'd need to store the authorization ID
                // For now, we'll simulate the refund logic
                SharedMoneyDTO estimatedFare = trip.getEstimatedFare().toSharedDTO();
                
                // In real implementation, we'd call:
                // boolean refundSuccess = paymentService.refund(authorizationId);
                // For now, we'll assume success and return the amount
                
                log.info("Processing refund for trip: {}, amount: {}", 
                    trip.getId().getValue(), estimatedFare.getAmount());
                    
                return estimatedFare;
                
            } catch (Exception e) {
                log.warn("Failed to process refund for trip: {}", trip.getId().getValue(), e);
                // Return zero amount if refund fails - business decision
                return createZeroAmount();
            }
        }
        
        return createZeroAmount();
    }
    
    private SharedMoneyDTO createZeroAmount() {
        SharedMoneyDTO zeroAmount = new SharedMoneyDTO();
        zeroAmount.setAmount(BigDecimal.ZERO);
        zeroAmount.setCurrency("USD"); // default currency
        return zeroAmount;
    }
    
    private void publishCancellationEvent(DomainTripEntity trip, String reason) {
        SharedTripEventDTO event = new SharedTripEventDTO();
        
        // Determine event type based on cancellation reason
        if ("MATCHING_TIMEOUT".equals(reason) || "SYSTEM_CANCELLED".equals(reason)) {
            event.setEventType("MATCH_TIMEOUT");
        } else {
            event.setEventType("TRIP_CANCELLED");
        }
        
        event.setTripId(trip.getId().getValue());
        event.setTimestamp(LocalDateTime.now().toString());
        event.setSource("trip-request-matching-shred");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("riderId", trip.getRider().getId());
        payload.put("reason", reason);
        payload.put("cancelledAt", LocalDateTime.now().toString());
        if (trip.getDriver() != null) {
            payload.put("driverId", trip.getDriver().getId());
        }
        event.setPayload(payload);
        
        eventPublisher.publishTripEvent(event);
        
        log.info("Published cancellation event for trip: {}, type: {}", 
            trip.getId().getValue(), event.getEventType());
    }
}