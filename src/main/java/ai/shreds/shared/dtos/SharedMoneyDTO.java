package ai.shreds.shared.dtos;

import ai.shreds.domain.value_objects.DomainMoneyValue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Monetary amount with currency, shared across layers for fare estimates, payment authorizations,
 * and refund amounts. Provides bidirectional conversion with the domain Money value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedMoneyDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency; // ISO 4217 currency code (USD, EUR, etc.)

    /* ===================== Domain Conversions ===================== */

    /**
     * Converts this DTO to a domain money value for business logic processing.
     */
    public DomainMoneyValue toDomainValue() {
        return new DomainMoneyValue(amount, currency);
    }

    /**
     * Factory method to create a SharedMoneyDTO from a domain money value.
     */
    public static SharedMoneyDTO fromDomainValue(DomainMoneyValue value) {
        if (value == null) {
            return null;
        }
        return SharedMoneyDTO.builder()
                .amount(value.getAmount())
                .currency(value.getCurrency())
                .build();
    }
}
