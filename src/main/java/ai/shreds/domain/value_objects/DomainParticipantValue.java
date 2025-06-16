package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

public class DomainParticipantValue {
    private final String id;
    private final String name;
    private final String phone;
    private final BigDecimal rating;

    public DomainParticipantValue(String id, String name, String phone, BigDecimal rating) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.rating = rating;
        validateRating();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getMaskedPhone() {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        int visibleDigits = 4;
        int maskedLength = phone.length() - visibleDigits;
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            masked.append("*");
        }
        masked.append(phone.substring(maskedLength));
        return masked.toString();
    }

    public void validateRating() {
        if (id == null || id.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Participant ID cannot be null or empty", null);
        }
        if (name == null || name.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Participant name cannot be null or empty", null);
        }
        if (name.length() > 100) {
            throw new DomainBusinessRuleViolationException("Participant name cannot exceed 100 characters", null);
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Participant phone cannot be null or empty", null);
        }
        if (rating == null) {
            throw new DomainBusinessRuleViolationException("Rating cannot be null", null);
        }
        if (rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0) {
            Map<String, Object> details = new HashMap<>();
            details.put("rating", rating);
            details.put("minimum", 1.0);
            details.put("maximum", 5.0);
            throw new DomainBusinessRuleViolationException("Rating must be between 1.0 and 5.0", details);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainParticipantValue that = (DomainParticipantValue) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(rating, that.rating);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, phone, rating);
    }
}
