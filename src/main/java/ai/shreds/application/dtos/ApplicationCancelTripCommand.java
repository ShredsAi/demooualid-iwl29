package ai.shreds.application.dtos;

import ai.shreds.domain.commands.DomainCancelTripCommand;
import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import lombok.Data;

@Data
public class ApplicationCancelTripCommand {
    private String tripId;
    private String reason;
    private String cancellationNote;
    private String cancelledBy;

    public DomainCancelTripCommand toDomainCommand() {
        return toDomainCommand(null);
    }

    public DomainCancelTripCommand toDomainCommand(DomainMoneyValue refundAmount) {
        return new DomainCancelTripCommand(
                tripId,
                mapToDomainCancellationReason(reason),
                cancelledBy != null ? cancelledBy : "SYSTEM", // Default, could be enhanced to track actual canceller
                cancellationNote,
                refundAmount
        );
    }
    
    private DomainCancellationReasonEnum mapToDomainCancellationReason(String reason) {
        if (reason == null) {
            return DomainCancellationReasonEnum.SYSTEM_CANCELLED;
        }
        
        switch (reason.toUpperCase()) {
            case "RIDER_CANCELLED":
                return DomainCancellationReasonEnum.RIDER_CANCELLED;
            case "DRIVER_CANCELLED":
                return DomainCancellationReasonEnum.DRIVER_CANCELLED;
            case "PAYMENT_FAILED":
                return DomainCancellationReasonEnum.PAYMENT_FAILED;
            case "MATCHING_TIMEOUT":
                return DomainCancellationReasonEnum.MATCHING_TIMEOUT;
            case "SYSTEM_CANCELLED":
            default:
                return DomainCancellationReasonEnum.SYSTEM_CANCELLED;
        }
    }
}