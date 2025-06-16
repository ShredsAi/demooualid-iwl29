package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationCancellationResultDTO {
    private String tripId;
    private String status;
    private String cancelledAt;
    private SharedMoneyDTO refundAmount;
    private Boolean refundProcessed;

    public static ApplicationCancellationResultDTO fromDomainEntity(DomainTripEntity entity, SharedMoneyDTO refundAmount) {
        ApplicationCancellationResultDTO dto = new ApplicationCancellationResultDTO();
        dto.setTripId(entity.getId().getValue());
        dto.setStatus(entity.getStatus().name());
        dto.setCancelledAt(entity.getCancellationInfo() != null ? 
            entity.getCancellationInfo().getCancelledAt().toString() : 
            LocalDateTime.now().toString());
        dto.setRefundAmount(refundAmount);
        dto.setRefundProcessed(refundAmount != null && refundAmount.getAmount().doubleValue() > 0);
        return dto;
    }

    public SharedCancellationResponseDTO toSharedResponse() {
        SharedCancellationResponseDTO response = new SharedCancellationResponseDTO();
        response.setTripId(tripId);
        response.setStatus(status);
        response.setCancelledAt(cancelledAt);
        response.setRefundAmount(refundAmount);
        response.setRefundProcessed(refundProcessed);
        return response;
    }
}