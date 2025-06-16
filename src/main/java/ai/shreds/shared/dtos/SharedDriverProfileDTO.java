package ai.shreds.shared.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Driver profile information returned by the Driver Service. Used to validate
 * driver availability and status during the matching process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedDriverProfileDTO {

    /** Unique identifier of the driver. */
    private String driverId;

    /** Driver account status (ACTIVE, SUSPENDED, INACTIVE). */
    private String status;

    /** Current online state (ONLINE, OFFLINE, BUSY). */
    private String onlineState;

    /** Type of vehicle (SEDAN, SUV, VAN, etc.). */
    private String vehicleType;

    /** Driver's current rating from 1.0 to 5.0. */
    private BigDecimal rating;

    /**
     * Determines if this driver is available to accept trip assignments.
     * A driver is considered available if they are online and have an active status.
     *
     * @return true if driver can accept trips, false otherwise
     */
    public boolean isAvailable() {
        return "ACTIVE".equalsIgnoreCase(status) && 
               "ONLINE".equalsIgnoreCase(onlineState);
    }
}