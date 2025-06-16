package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainLocationValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DomainCreateTripCommand {
    private final String riderId;
    private final String riderName;
    private final String riderPhone;
    private final BigDecimal riderRating;
    private final DomainLocationValue pickupLocation;
    private final DomainLocationValue dropoffLocation;
    private final LocalDateTime scheduledFor;
    private final DomainMoneyValue estimatedFare;
    private final Map<String, String> metadata;

    public DomainCreateTripCommand(String riderId, String riderName, String riderPhone, BigDecimal riderRating,
                                   DomainLocationValue pickupLocation, DomainLocationValue dropoffLocation,
                                   LocalDateTime scheduledFor, DomainMoneyValue estimatedFare, Map<String, String> metadata) {
        this.riderId = riderId;
        this.riderName = riderName;
        this.riderPhone = riderPhone;
        this.riderRating = riderRating;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.scheduledFor = scheduledFor;
        this.estimatedFare = estimatedFare;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public String getRiderId() {
        return riderId;
    }

    public String getRiderName() {
        return riderName;
    }

    public String getRiderPhone() {
        return riderPhone;
    }

    public BigDecimal getRiderRating() {
        return riderRating;
    }

    public DomainLocationValue getPickupLocation() {
        return pickupLocation;
    }

    public DomainLocationValue getDropoffLocation() {
        return dropoffLocation;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public DomainMoneyValue getEstimatedFare() {
        return estimatedFare;
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
}
