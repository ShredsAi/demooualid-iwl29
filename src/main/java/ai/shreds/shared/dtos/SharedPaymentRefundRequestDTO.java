package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for refunding a previously authorized payment.
 * Used when trips are cancelled or matching fails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentRefundRequestDTO {

    /** Authorization ID to be refunded. */
    private String authorizationId;
}