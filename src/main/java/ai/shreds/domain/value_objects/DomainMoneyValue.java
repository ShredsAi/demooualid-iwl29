package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

public class DomainMoneyValue {
    private final BigDecimal amount;
    private final String currency;

    public DomainMoneyValue(BigDecimal amount, String currency) {
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_EVEN) : null;
        this.currency = currency;
        validateCurrency();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void validateCurrency() {
        if (amount == null) {
            throw new DomainBusinessRuleViolationException("Amount cannot be null", null);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainBusinessRuleViolationException("Amount cannot be negative", null);
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Currency cannot be null or empty", null);
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new DomainBusinessRuleViolationException("Currency must be a valid ISO 4217 code (3 uppercase letters)", null);
        }
    }

    public DomainMoneyValue add(DomainMoneyValue other) {
        validateSameCurrency(other);
        return new DomainMoneyValue(this.amount.add(other.amount), this.currency);
    }

    public DomainMoneyValue subtract(DomainMoneyValue other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            Map<String, Object> details = new HashMap<>();
            details.put("amount1", this.amount);
            details.put("amount2", other.amount);
            details.put("result", result);
            throw new DomainBusinessRuleViolationException("Result of subtraction cannot be negative", details);
        }
        return new DomainMoneyValue(result, this.currency);
    }

    public DomainMoneyValue multiply(BigDecimal factor) {
        if (factor == null) {
            throw new DomainBusinessRuleViolationException("Multiplicand cannot be null", null);
        }
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainBusinessRuleViolationException("Multiplicand cannot be negative", null);
        }
        return new DomainMoneyValue(this.amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN), this.currency);
    }

    private void validateSameCurrency(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            Map<String, Object> details = new HashMap<>();
            details.put("currency1", this.currency);
            details.put("currency2", other.currency);
            throw new DomainBusinessRuleViolationException("Cannot perform operation on different currencies", details);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainMoneyValue that = (DomainMoneyValue) o;
        return Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
