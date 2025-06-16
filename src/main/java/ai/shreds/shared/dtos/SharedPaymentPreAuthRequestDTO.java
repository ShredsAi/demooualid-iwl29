package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for pre-authorizing payment from the rider's payment method.
 * Sent to Payment Service to secure funds before driver matching begins.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentPreAuthRequestDTO {

    /** Rider ID for whom to authorize payment. */
    private String riderId;

    /** Amount to pre-authorize. */
    private BigDecimal amount;

    /** Currency code for the amount (ISO 4217). */
    private String currency;

    /** Trip correlation ID for tracking the authorization. */
    private String tripCorrelationId;
}