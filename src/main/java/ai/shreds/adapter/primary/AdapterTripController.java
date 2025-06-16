package ai.shreds.adapter.primary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ai.shreds.shared.dtos.SharedTripRequestDTO;
import ai.shreds.shared.dtos.SharedTripResponseDTO;
import ai.shreds.shared.dtos.SharedCancellationRequestDTO;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.application.dtos.ApplicationCreateTripCommand;
import ai.shreds.application.dtos.ApplicationTripDTO;
import ai.shreds.application.dtos.ApplicationCancelTripCommand;
import ai.shreds.application.dtos.ApplicationCancellationResultDTO;
import ai.shreds.application.ports.ApplicationCreateTripInputPort;
import ai.shreds.application.ports.ApplicationCancelTripInputPort;

@RestController
@RequestMapping("/api/v1/trips")
public class AdapterTripController {

    private final ApplicationCreateTripInputPort applicationCreateTripPort;
    private final ApplicationCancelTripInputPort applicationCancelTripPort;

    public AdapterTripController(ApplicationCreateTripInputPort applicationCreateTripPort,
                                 ApplicationCancelTripInputPort applicationCancelTripPort) {
        this.applicationCreateTripPort = applicationCreateTripPort;
        this.applicationCancelTripPort = applicationCancelTripPort;
    }

    @PostMapping
    public ResponseEntity<SharedTripResponseDTO> createTrip(@RequestBody SharedTripRequestDTO request) {
        ApplicationCreateTripCommand createCommand = mapToCreateTripCommand(request);
        ApplicationTripDTO tripDTO = applicationCreateTripPort.createTrip(createCommand);
        SharedTripResponseDTO responseDTO = mapToTripResponse(tripDTO);
        return ResponseEntity.status(201).body(responseDTO);
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<SharedCancellationResponseDTO> cancelTrip(@PathVariable String tripId, @RequestBody SharedCancellationRequestDTO request) {
        ApplicationCancelTripCommand cancelCommand = mapToCancelTripCommand(tripId, request);
        ApplicationCancellationResultDTO resultDTO = applicationCancelTripPort.cancelTrip(cancelCommand);
        SharedCancellationResponseDTO responseDTO = mapToCancellationResponse(resultDTO);
        return ResponseEntity.ok(responseDTO);
    }

    private ApplicationCreateTripCommand mapToCreateTripCommand(SharedTripRequestDTO request) {
        return request.toApplicationCommand();
    }

    private SharedTripResponseDTO mapToTripResponse(ApplicationTripDTO tripDTO) {
        return tripDTO.toSharedResponse();
    }

    private ApplicationCancelTripCommand mapToCancelTripCommand(String tripId, SharedCancellationRequestDTO request) {
        return request.toApplicationCommand(tripId);
    }

    private SharedCancellationResponseDTO mapToCancellationResponse(ApplicationCancellationResultDTO resultDTO) {
        return resultDTO.toSharedResponse();
    }
}
