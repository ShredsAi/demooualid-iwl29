package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationCreateTripCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Incoming request from a rider to create a new trip. This object is exposed on the public REST API
 * and therefore must remain technology-agnostic. Validation annotations ensure early rejection of
 * malformed payloads before business logic is executed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTripRequestDTO {

    /** Reference to the rider placing the request. */
    @NotBlank(message = "Rider ID is required")
    private String riderId;

    /** Where the rider should be picked up. */
    @NotNull(message = "Pickup location is required")
    @Valid
    private SharedLocationDTO pickupLocation;

    /** Desired drop-off location. */
    @NotNull(message = "Drop-off location is required")
    @Valid
    private SharedLocationDTO dropoffLocation;

    /** Optional ISO-8601 scheduled time for the ride. Null indicates immediate pickup. */
    private String scheduledFor;

    /** Arbitrary key/value metadata coming from mobile client (e.g. wheelchair, car seat). */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Converts this public DTO into an application-level command understood by the Application layer. The
     * Application layer will further convert to a Domain command as needed.
     *
     * @return fully populated ApplicationCreateTripCommand instance
     */
    public ApplicationCreateTripCommand toApplicationCommand() {
        return ApplicationCreateTripCommand.builder()
                .riderId(riderId)
                .pickupLocation(pickupLocation)
                .dropoffLocation(dropoffLocation)
                .scheduledFor(scheduledFor)
                .metadata(metadata)
                .build();
    }
}
