package ai.shreds.adapters.primary;

import ai.shreds.shared.dtos.*;
import ai.shreds.shared.exceptions.*;
import ai.shreds.application.ports.ApplicationCreateTripInputPort;
import ai.shreds.application.ports.ApplicationCancelTripInputPort;
import ai.shreds.application.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdapterTripController {

    private final ApplicationCreateTripInputPort applicationCreateTripPort;
    private final ApplicationCancelTripInputPort applicationCancelTripPort;

    @PostMapping
    public ResponseEntity<SharedTripResponseDTO> createTrip(@Valid @RequestBody SharedTripRequestDTO request) {
        log.info("Received trip creation request for rider: {}", request.getRiderId());
        
        try {
            ApplicationCreateTripCommand command = mapToCreateTripCommand(request);
            ApplicationTripDTO tripDTO = applicationCreateTripPort.createTrip(command);
            SharedTripResponseDTO response = mapToTripResponse(tripDTO);
            
            log.info("Trip created successfully with ID: {}", response.getTripId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SharedValidationException e) {
            log.error("Validation error creating trip: {}", e.getMessage());
            throw e;
        } catch (SharedPaymentException e) {
            log.error("Payment error creating trip: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating trip", e);
            throw new RuntimeException("Failed to create trip", e);
        }
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<SharedCancellationResponseDTO> cancelTrip(
            @PathVariable @NotNull String tripId,
            @Valid @RequestBody SharedCancellationRequestDTO request) {
        log.info("Received cancellation request for trip: {}", tripId);
        
        try {
            ApplicationCancelTripCommand command = mapToCancelTripCommand(tripId, request);
            ApplicationCancellationResultDTO result = applicationCancelTripPort.cancelTrip(command);
            SharedCancellationResponseDTO response = mapToCancellationResponse(result);
            
            log.info("Trip cancelled successfully: {}", tripId);
            return ResponseEntity.ok(response);
        } catch (SharedNotFoundException e) {
            log.error("Trip not found: {}", tripId);
            throw e;
        } catch (SharedConflictException e) {
            log.error("Conflict cancelling trip: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error cancelling trip: {}", tripId, e);
            throw new RuntimeException("Failed to cancel trip", e);
        }
    }

    private ApplicationCreateTripCommand mapToCreateTripCommand(SharedTripRequestDTO request) {
        return request.toApplicationCommand();
    }

    private SharedTripResponseDTO mapToTripResponse(ApplicationTripDTO trip) {
        return SharedTripResponseDTO.fromApplicationDTO(trip);
    }

    private ApplicationCancelTripCommand mapToCancelTripCommand(String tripId, SharedCancellationRequestDTO request) {
        return request.toApplicationCommand(tripId);
    }

    private SharedCancellationResponseDTO mapToCancellationResponse(ApplicationCancellationResultDTO result) {
        return SharedCancellationResponseDTO.fromApplicationDTO(result);
    }
}