package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rider profile information returned by the Rider Service. Used to validate
 * rider eligibility before allowing trip creation. Contains status, rating,
 * and any flags or suspension information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedRiderProfileDTO {

    /** Unique identifier of the rider. */
    private String riderId;

    /** Current account status (ACTIVE, SUSPENDED, INACTIVE). */
    private String status;

    /** Rider's current rating from 1.0 to 5.0. */
    private BigDecimal rating;

    /** List of special flags or restrictions on the account. */
    private List<String> flags;

    /** Reason for suspension if status is SUSPENDED. */
    private String suspensionReason;

    /**
     * Determines if this rider is eligible to request trips based on their
     * account status and suspension state.
     *
     * @return true if rider can request trips, false otherwise
     */
    public boolean isEligible() {
        return "ACTIVE".equalsIgnoreCase(status) && 
               (suspensionReason == null || suspensionReason.trim().isEmpty());
    }
}