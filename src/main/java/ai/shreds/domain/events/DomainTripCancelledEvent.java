package ai.shreds.domain.events;

import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DomainTripCancelledEvent implements DomainDomainEvent {
    private final String aggregateId;
    private final String eventType = "CANCELLED";
    private final LocalDateTime occurredAt;
    private final Map<String, Object> payload;

    public DomainTripCancelledEvent(String tripId, DomainCancellationReasonEnum reason, LocalDateTime cancelledAt) {
        this.aggregateId = tripId;
        this.occurredAt = cancelledAt;
        this.payload = new HashMap<>();
        this.payload.put("reason", reason.name());
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public Map<String, Object> getPayload() {
        return new HashMap<>(payload);
    }
}
