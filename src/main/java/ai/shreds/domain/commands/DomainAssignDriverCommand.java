package ai.shreds.domain.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DomainAssignDriverCommand {
    private final String tripId;
    private final String driverId;
    private final String driverName;
    private final String driverPhone;
    private final BigDecimal driverRating;
    private final LocalDateTime estimatedPickupTime;

    public DomainAssignDriverCommand(String tripId, String driverId, String driverName,
                                    String driverPhone, BigDecimal driverRating,
                                    LocalDateTime estimatedPickupTime) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.driverRating = driverRating;
        this.estimatedPickupTime = estimatedPickupTime;
    }

    public String getTripId() {
        return tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public BigDecimal getDriverRating() {
        return driverRating;
    }

    public LocalDateTime getEstimatedPickupTime() {
        return estimatedPickupTime;
    }
}
