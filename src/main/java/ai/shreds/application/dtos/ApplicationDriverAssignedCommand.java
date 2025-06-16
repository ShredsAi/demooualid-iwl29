package ai.shreds.application.dtos;

import ai.shreds.domain.commands.DomainAssignDriverCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDriverAssignedCommand {
    private String tripId;
    private String driverId;
    private String driverName;
    private String driverPhone;
    private BigDecimal driverRating;
    private String estimatedPickupTime;
    private String correlationId;

    public DomainAssignDriverCommand toDomainCommand() {
        return new DomainAssignDriverCommand(
                tripId,
                driverId,
                driverName != null ? driverName : "Driver-" + driverId, // Default name, will be enriched by service
                driverPhone != null ? driverPhone : "+1-XXX-XXX-XXXX", // Default phone, will be enriched by service
                driverRating != null ? driverRating : BigDecimal.valueOf(4.5), // Default rating, will be enriched by service
                estimatedPickupTime != null ? LocalDateTime.parse(estimatedPickupTime) : LocalDateTime.now().plusMinutes(10)
        );
    }
}