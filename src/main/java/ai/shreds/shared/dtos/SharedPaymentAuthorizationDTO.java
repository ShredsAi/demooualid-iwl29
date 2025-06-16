package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
@Slf4j
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
        log.debug("Checking payment authorization validity - Status: {}, Expiry: {}", status, expiry);
        
        if (!"AUTHORIZED".equalsIgnoreCase(status)) {
            log.debug("Payment authorization invalid due to status: {}", status);
            return false;
        }
        
        if (expiry == null || expiry.trim().isEmpty()) {
            log.debug("Payment authorization invalid due to missing expiry");
            return false;
        }
        
        try {
            // Try parsing with timezone first (ISO format with Z or offset)
            if (expiry.contains("Z") || expiry.contains("+") || expiry.matches(".*-\\d{2}:\\d{2}$")) {
                log.debug("Parsing expiry with timezone format: {}", expiry);
                OffsetDateTime expiryTime = OffsetDateTime.parse(expiry, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                boolean valid = OffsetDateTime.now().isBefore(expiryTime);
                log.debug("Payment authorization valid: {}, current time: {}, expiry time: {}", 
                    valid, OffsetDateTime.now(), expiryTime);
                return valid;
            } else {
                // Fallback to local date time format
                log.debug("Parsing expiry with local date time format: {}", expiry);
                LocalDateTime expiryTime = LocalDateTime.parse(expiry, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                boolean valid = LocalDateTime.now().isBefore(expiryTime);
                log.debug("Payment authorization valid: {}, current time: {}, expiry time: {}", 
                    valid, LocalDateTime.now(), expiryTime);
                return valid;
            }
        } catch (Exception e) {
            log.error("Payment authorization invalid due to parsing error: {}", e.getMessage(), e);
            return false;
        }
    }
}