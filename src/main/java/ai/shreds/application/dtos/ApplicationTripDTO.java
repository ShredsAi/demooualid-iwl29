package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.shared.dtos.SharedLocationDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.dtos.SharedTripResponseDTO;
import lombok.Data;

import java.util.Map;

@Data
public class ApplicationTripDTO {
    private String tripId;
    private String status;
    private String riderId;
    private String driverId;
    private SharedLocationDTO pickupLocation;
    private SharedLocationDTO dropoffLocation;
    private String requestedAt;
    private String scheduledFor;
    private SharedMoneyDTO estimatedFare;
    private Map<String, String> metadata;

    public static ApplicationTripDTO fromDomainEntity(DomainTripEntity entity) {
        ApplicationTripDTO dto = new ApplicationTripDTO();
        dto.setTripId(entity.getId().getValue());
        dto.setStatus(entity.getStatus().name());
        dto.setRiderId(entity.getRider().getId());
        dto.setDriverId(entity.getDriver() != null ? entity.getDriver().getId() : null);
        dto.setPickupLocation(entity.getPickupLocation().toSharedDTO());
        dto.setDropoffLocation(entity.getDropoffLocation().toSharedDTO());
        dto.setRequestedAt(entity.getRequestedAt().toString());
        dto.setScheduledFor(entity.getScheduledFor() != null ? entity.getScheduledFor().toString() : null);
        dto.setEstimatedFare(entity.getEstimatedFare().toSharedDTO());
        dto.setMetadata(entity.getMetadata());
        return dto;
    }

    public SharedTripResponseDTO toSharedResponse() {
        SharedTripResponseDTO response = new SharedTripResponseDTO();
        response.setTripId(tripId);
        response.setStatus(status);
        response.setEstimatedFare(estimatedFare);
        response.setRequestedAt(requestedAt);
        return response;
    }
}