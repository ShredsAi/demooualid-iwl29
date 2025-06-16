package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal message DTO sent to Trip Execution Shred via Spring Integration
 * when a trip has been successfully matched with a driver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTripMatchedMessageDTO {

    /** Unique identifier of the trip that was matched. */
    private String tripId;

    /** ID of the driver assigned to this trip. */
    private String driverId;

    /** Current status of the trip (should be MATCHED). */
    private String tripStatus;

    /** ISO-8601 timestamp when the matching occurred. */
    private String matchedAt;
}