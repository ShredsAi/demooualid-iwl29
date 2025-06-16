package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO sent to the Pricing Service to get fare estimates.
 * Contains pickup/dropoff locations and any metadata affecting pricing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPricingRequestDTO {

    /** Pickup location for fare calculation. */
    private SharedLocationDTO pickupLocation;

    /** Drop-off location for distance/duration calculation. */
    private SharedLocationDTO dropoffLocation;

    /** Additional metadata affecting pricing (time of day, vehicle type preferences, etc.). */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
}