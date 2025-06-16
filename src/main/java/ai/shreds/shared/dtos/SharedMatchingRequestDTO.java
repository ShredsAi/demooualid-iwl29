package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO sent to the Matching Service via Kafka to initiate driver matching.
 * Contains all information needed for the matching algorithm to find an appropriate driver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedMatchingRequestDTO {

    /** Unique identifier of the trip requiring a driver match. */
    private String tripId;

    /** Pickup location for driver matching algorithm. */
    private SharedLocationDTO pickupLocation;

    /** Drop-off location to estimate trip duration and find nearby drivers. */
    private SharedLocationDTO dropoffLocation;

    /** Rider ID for driver preference matching and history. */
    private String riderId;

    /** Additional metadata for matching preferences. */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /** ISO-8601 timestamp when matching was requested. */
    private String requestedAt;

    /** Correlation ID for request/reply pattern tracking. */
    private String correlationId;
}