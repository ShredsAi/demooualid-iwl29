package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fare estimate information returned by the Pricing Service. Contains
 * the calculated fare amount, surge multiplier, and trip duration/distance estimates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedFareEstimateDTO {

    /** Base fare amount before surge multiplier. */
    private BigDecimal amount;

    /** Currency code (ISO 4217) for the fare amount. */
    private String currency;

    /** Current surge multiplier applied to base fare. */
    @Builder.Default
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    /** Estimated trip duration in seconds. */
    private Long estimatedDuration;

    /** Estimated trip distance in kilometers. */
    private BigDecimal estimatedDistance;

    /**
     * Converts this fare estimate to a SharedMoneyDTO representing the final
     * fare amount (including surge multiplier).
     *
     * @return SharedMoneyDTO with the calculated final fare
     */
    public SharedMoneyDTO toMoney() {
        BigDecimal finalAmount = amount;
        if (surgeMultiplier != null && surgeMultiplier.compareTo(BigDecimal.ZERO) > 0) {
            finalAmount = amount.multiply(surgeMultiplier);
        }
        return SharedMoneyDTO.builder()
                .amount(finalAmount)
                .currency(currency)
                .build();
    }
}