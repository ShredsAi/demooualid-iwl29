package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationCancelTripCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a cancellation request from a rider or system. This DTO captures the reason for
 * cancellation and any additional notes that should be recorded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedCancellationRequestDTO {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    private String cancellationNote;

    /**
     * Converts this shared DTO to an application-layer command that can process the cancellation.
     *
     * @param tripId the trip to be cancelled
     * @return ApplicationCancelTripCommand for the application layer to process
     */
    public ApplicationCancelTripCommand toApplicationCommand(String tripId) {
        return ApplicationCancelTripCommand.builder()
                .tripId(tripId)
                .reason(reason)
                .cancellationNote(cancellationNote)
                .build();
    }
}
