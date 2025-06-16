package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic trip event DTO for publishing lifecycle events to Kafka.
 * Used for broadcasting trip state changes to other services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTripEventDTO {

    /** Type of event (TRIP_REQUESTED, DRIVER_MATCHED, MATCH_TIMEOUT, etc.). */
    private String eventType;

    /** Trip ID this event relates to. */
    private String tripId;

    /** ISO-8601 timestamp when the event occurred. */
    private String timestamp;

    /** Event-specific payload data. */
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    /** Source service that generated this event. */
    private String source;
}