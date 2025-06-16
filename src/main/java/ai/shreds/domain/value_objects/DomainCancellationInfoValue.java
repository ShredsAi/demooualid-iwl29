package ai.shreds.domain.value_objects;

import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DomainCancellationInfoValue {
    private final DomainCancellationReasonEnum reason;
    private final String cancelledBy;
    private final LocalDateTime cancelledAt;
    private final DomainMoneyValue refundAmount;

    public DomainCancellationInfoValue(DomainCancellationReasonEnum reason, String cancelledBy, 
                                      LocalDateTime cancelledAt, DomainMoneyValue refundAmount) {
        this.reason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.refundAmount = refundAmount;
        validateTimestamp();
    }

    public DomainCancellationReasonEnum getReason() {
        return reason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public DomainMoneyValue getRefundAmount() {
        return refundAmount;
    }

    public void validateTimestamp() {
        if (reason == null) {
            throw new DomainBusinessRuleViolationException("Cancellation reason cannot be null", null);
        }
        if (cancelledBy == null || cancelledBy.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Cancelling entity ID cannot be null or empty", null);
        }
        if (cancelledAt == null) {
            throw new DomainBusinessRuleViolationException("Cancellation timestamp cannot be null", null);
        }
        if (cancelledAt.isAfter(LocalDateTime.now())) {
            Map<String, Object> details = new HashMap<>();
            details.put("cancelledAt", cancelledAt);
            details.put("currentTime", LocalDateTime.now());
            throw new DomainBusinessRuleViolationException("Cancellation timestamp cannot be in the future", details);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCancellationInfoValue that = (DomainCancellationInfoValue) o;
        return reason == that.reason &&
                Objects.equals(cancelledBy, that.cancelledBy) &&
                Objects.equals(cancelledAt, that.cancelledAt) &&
                Objects.equals(refundAmount, that.refundAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason, cancelledBy, cancelledAt, refundAmount);
    }
}
