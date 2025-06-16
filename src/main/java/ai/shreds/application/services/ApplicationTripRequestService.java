package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationCreateTripCommand;
import ai.shreds.application.dtos.ApplicationTripDTO;
import ai.shreds.application.exceptions.ApplicationRiderIneligibleException;
import ai.shreds.application.exceptions.ApplicationPaymentFailedException;
import ai.shreds.application.exceptions.ApplicationServiceException;
import ai.shreds.application.ports.ApplicationCreateTripInputPort;
import ai.shreds.application.ports.ApplicationEventPublisherOutputPort;
import ai.shreds.application.ports.ApplicationMatchingServiceOutputPort;
import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.application.ports.ApplicationPricingServiceOutputPort;
import ai.shreds.application.ports.ApplicationRiderServiceOutputPort;
import ai.shreds.application.ports.ApplicationTripRepositoryOutputPort;
import ai.shreds.domain.commands.DomainCreateTripCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.ports.DomainTripServiceInputPort;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.shared.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationTripRequestService implements ApplicationCreateTripInputPort {

    private final ApplicationTripRepositoryOutputPort tripRepository;
    private final ApplicationRiderServiceOutputPort riderService;
    private final ApplicationPricingServiceOutputPort pricingService;
    private final ApplicationPaymentServiceOutputPort paymentService;
    private final ApplicationMatchingServiceOutputPort matchingService;
    private final ApplicationEventPublisherOutputPort eventPublisher;
    private final DomainTripServiceInputPort domainTripService;

    @Override
    @Transactional
    public ApplicationTripDTO createTrip(ApplicationCreateTripCommand command) {
        log.info("Creating trip for rider: {}", command.getRiderId());
        
        try {
            // Step 1: Validate rider eligibility and get rider details
            SharedRiderProfileDTO riderProfile = validateRiderEligibility(command.getRiderId());
            
            // Step 2: Enrich command with rider details
            enrichCommandWithRiderDetails(command, riderProfile);
            
            // Step 3: Estimate fare
            SharedMoneyDTO estimatedFare = estimateFare(
                command.getPickupLocation(), 
                command.getDropoffLocation(), 
                command.getMetadata()
            );
            
            // Step 4: Pre-authorize payment
            String authorizationId = preAuthorizePayment(
                command.getRiderId(), 
                estimatedFare, 
                UUID.randomUUID().toString() // trip correlation id
            );
            
            // Step 5: Create domain command with estimated fare
            DomainMoneyValue domainEstimatedFare = DomainMoneyValue.fromSharedDTO(estimatedFare);
            DomainCreateTripCommand domainCommand = command.toDomainCommand(domainEstimatedFare);
            
            // Step 6: Create trip through domain service
            DomainTripEntity trip = domainTripService.createTrip(domainCommand);
            
            // Step 7: Start matching process
            startMatching(trip);
            
            // Step 8: Publish trip requested event
            publishTripRequested(trip);
            
            // Step 9: Convert to application DTO and return
            return ApplicationTripDTO.fromDomainEntity(trip);
            
        } catch (Exception e) {
            log.error("Error creating trip for rider: {}", command.getRiderId(), e);
            if (e instanceof ApplicationRiderIneligibleException || 
                e instanceof ApplicationPaymentFailedException) {
                throw e;
            }
            throw new ApplicationServiceException("Failed to create trip", e);
        }
    }
    
    private SharedRiderProfileDTO validateRiderEligibility(String riderId) {
        try {
            SharedRiderProfileDTO riderProfile = riderService.validateRider(riderId);
            if (!riderProfile.isEligible()) {
                throw new ApplicationRiderIneligibleException(
                    riderId, 
                    riderProfile.getSuspensionReason() != null ? 
                        riderProfile.getSuspensionReason() : "Rider not eligible"
                );
            }
            return riderProfile;
        } catch (Exception e) {
            if (e instanceof ApplicationRiderIneligibleException) {
                throw e;
            }
            throw new ApplicationServiceException("Failed to validate rider", e);
        }
    }
    
    private void enrichCommandWithRiderDetails(ApplicationCreateTripCommand command, SharedRiderProfileDTO riderProfile) {
        // Enrich command with rider details from profile
        // In a real scenario, you might have additional rider details in the profile
        // For now, using basic fallback values
        if (command.getRiderName() == null) {
            command.setRiderName("Rider " + command.getRiderId().substring(0, 8));
        }
        if (command.getRiderPhone() == null) {
            command.setRiderPhone("+1234567890"); // Should come from rider service
        }
        if (command.getRiderRating() == null) {
            command.setRiderRating(riderProfile.getRating());
        }
    }
    
    private SharedMoneyDTO estimateFare(SharedLocationDTO pickup, SharedLocationDTO dropoff, 
                                        java.util.Map<String, String> metadata) {
        try {
            SharedFareEstimateDTO fareEstimate = pricingService.estimateFare(pickup, dropoff, metadata);
            return fareEstimate.toMoney();
        } catch (Exception e) {
            throw new ApplicationServiceException("Failed to estimate fare", e);
        }
    }
    
    private String preAuthorizePayment(String riderId, SharedMoneyDTO amount, String tripId) {
        try {
            SharedPaymentAuthorizationDTO authorization = paymentService.preAuthorize(
                riderId, amount, tripId
            );
            if (!authorization.isValid()) {
                throw new ApplicationPaymentFailedException(
                    riderId, amount, "Payment authorization failed"
                );
            }
            return authorization.getAuthorizationId();
        } catch (Exception e) {
            if (e instanceof ApplicationPaymentFailedException) {
                throw e;
            }
            throw new ApplicationPaymentFailedException(
                riderId, amount, "Payment authorization error: " + e.getMessage()
            );
        }
    }
    
    private void startMatching(DomainTripEntity trip) {
        SharedMatchingRequestDTO matchingRequest = new SharedMatchingRequestDTO();
        matchingRequest.setTripId(trip.getId().getValue());
        matchingRequest.setPickupLocation(trip.getPickupLocation().toSharedDTO());
        matchingRequest.setDropoffLocation(trip.getDropoffLocation().toSharedDTO());
        matchingRequest.setRiderId(trip.getRider().getId());
        matchingRequest.setMetadata(trip.getMetadata());
        matchingRequest.setRequestedAt(trip.getRequestedAt().toString());
        matchingRequest.setCorrelationId(UUID.randomUUID().toString());
        
        matchingService.requestMatching(matchingRequest);
    }
    
    private void publishTripRequested(DomainTripEntity trip) {
        SharedTripEventDTO event = new SharedTripEventDTO();
        event.setEventType("TRIP_REQUESTED");
        event.setTripId(trip.getId().getValue());
        event.setTimestamp(trip.getRequestedAt().toString());
        event.setSource("trip-request-matching-shred");
        
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("riderId", trip.getRider().getId());
        payload.put("pickupLocation", trip.getPickupLocation().toSharedDTO());
        payload.put("dropoffLocation", trip.getDropoffLocation().toSharedDTO());
        payload.put("estimatedFare", trip.getEstimatedFare().toSharedDTO());
        event.setPayload(payload);
        
        eventPublisher.publishTripEvent(event);
    }
}