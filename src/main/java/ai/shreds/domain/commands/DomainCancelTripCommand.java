package ai.shreds.domain.commands;

import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import ai.shreds.domain.value_objects.DomainMoneyValue;

public class DomainCancelTripCommand {
    private final String tripId;
    private final DomainCancellationReasonEnum reason;
    private final String cancelledBy;
    private final String cancellationNote;
    private final DomainMoneyValue refundAmount;

    public DomainCancelTripCommand(String tripId, DomainCancellationReasonEnum reason, 
                                  String cancelledBy, String cancellationNote,
                                  DomainMoneyValue refundAmount) {
        this.tripId = tripId;
        this.reason = reason;
        this.cancelledBy = cancelledBy;
        this.cancellationNote = cancellationNote;
        this.refundAmount = refundAmount;
    }

    public String getTripId() {
        return tripId;
    }

    public DomainCancellationReasonEnum getReason() {
        return reason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public DomainMoneyValue getRefundAmount() {
        return refundAmount;
    }
}
