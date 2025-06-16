package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationMatchingTimeoutCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO representing a matching timeout notification from the Matching Service.
 * This event indicates that no driver could be found within the configured time limit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedMatchingTimeoutEventDTO {

    /** Event type identifier for routing. */
    private String eventType;

    /** The trip ID that timed out during matching. */
    private String tripId;

    /** Reason for the timeout (e.g., "NO_DRIVERS_AVAILABLE", "HIGH_DEMAND"). */
    private String reason;

    /** Duration in milliseconds before timeout occurred. */
    private Long timeoutDuration;

    /** Correlation ID for request/reply tracking. */
    private String correlationId;

    /**
     * Converts this Kafka event DTO to an application command for processing matching timeout.
     *
     * @return ApplicationMatchingTimeoutCommand for the application layer to process
     */
    public ApplicationMatchingTimeoutCommand toApplicationCommand() {
        return ApplicationMatchingTimeoutCommand.builder()
                .tripId(tripId)
                .reason(reason)
                .timeoutDuration(timeoutDuration)
                .correlationId(correlationId)
                .build();
    }
}