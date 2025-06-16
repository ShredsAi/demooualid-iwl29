package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationDriverAssignedCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO representing successful driver assignment from the Matching Service.
 * This event transitions trips from MATCHING to MATCHED state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedDriverAssignedEventDTO {

    /** Event type identifier for routing. */
    private String eventType;

    /** The trip ID that has been matched with a driver. */
    private String tripId;

    /** The ID of the assigned driver. */
    private String driverId;

    /** ISO-8601 estimated pickup time from the driver. */
    private String estimatedPickupTime;

    /** Correlation ID for request/reply tracking. */
    private String correlationId;

    /**
     * Converts this Kafka event DTO to an application command for processing driver assignment.
     *
     * @return ApplicationDriverAssignedCommand for the application layer to process
     */
    public ApplicationDriverAssignedCommand toApplicationCommand() {
        return ApplicationDriverAssignedCommand.builder()
                .tripId(tripId)
                .driverId(driverId)
                .estimatedPickupTime(estimatedPickupTime)
                .correlationId(correlationId)
                .build();
    }
}
