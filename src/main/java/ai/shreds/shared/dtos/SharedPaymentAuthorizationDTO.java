package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payment authorization information returned by the Payment Service after
 * successful pre-authorization of funds. Contains authorization details
 * and expiry information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentAuthorizationDTO {

    /** Unique authorization identifier from the payment service. */
    private String authorizationId;

    /** Rider ID for whom the authorization was created. */
    private String riderId;

    /** The amount that was authorized. */
    private SharedMoneyDTO amount;

    /** ISO-8601 timestamp when the authorization expires. */
    private String expiry;

    /** Current status of the authorization (AUTHORIZED, EXPIRED, CANCELLED). */
    private String status;

    /**
     * Determines if this authorization is still valid based on its status
     * and expiry time.
     *
     * @return true if authorization is valid and not expired, false otherwise
     */
    public boolean isValid() {
        if (!"AUTHORIZED".equalsIgnoreCase(status)) {
            return false;
        }
        
        if (expiry == null || expiry.trim().isEmpty()) {
            return false;
        }
        
        try {
            LocalDateTime expiryTime = LocalDateTime.parse(expiry, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return LocalDateTime.now().isBefore(expiryTime);
        } catch (Exception e) {
            return false;
        }
    }
}