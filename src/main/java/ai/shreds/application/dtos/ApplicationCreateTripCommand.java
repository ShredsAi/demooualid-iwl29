package ai.shreds.application.dtos;

import ai.shreds.domain.commands.DomainCreateTripCommand;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.shared.dtos.SharedLocationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateTripCommand {
    private String riderId;
    private String riderName;
    private String riderPhone;
    private BigDecimal riderRating;
    private SharedLocationDTO pickupLocation;
    private SharedLocationDTO dropoffLocation;
    private String scheduledFor;
    private Map<String, String> metadata;

    public DomainCreateTripCommand toDomainCommand() {
        return toDomainCommand(null);
    }

    public DomainCreateTripCommand toDomainCommand(DomainMoneyValue estimatedFare) {
        return new DomainCreateTripCommand(
                riderId,
                riderName,
                riderPhone,
                riderRating,
                pickupLocation.toDomainValue(),
                dropoffLocation.toDomainValue(),
                scheduledFor != null ? LocalDateTime.parse(scheduledFor) : null,
                estimatedFare,
                metadata
        );
    }
}