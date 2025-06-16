package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import java.util.UUID;

public class DomainTripIdValue {
    private final String value;

    public DomainTripIdValue(String value) {
        this.value = value;
        validate();
    }

    public String getValue() {
        return value;
    }

    private void validate() {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Trip ID cannot be null or empty", null);
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new DomainBusinessRuleViolationException("Trip ID must be a valid UUID", null);
        }
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainTripIdValue that = (DomainTripIdValue) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
