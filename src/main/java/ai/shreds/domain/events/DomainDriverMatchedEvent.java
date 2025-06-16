package ai.shreds.domain.events;

import ai.shreds.domain.events.DomainDomainEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DomainDriverMatchedEvent implements DomainDomainEvent {
    private final String aggregateId;
    private final String eventType = "DRIVER_MATCHED";
    private final LocalDateTime occurredAt;
    private final Map<String, Object> payload;

    public DomainDriverMatchedEvent(String tripId, String driverId, LocalDateTime matchedAt) {
        this.aggregateId = tripId;
        this.occurredAt = matchedAt;
        this.payload = new HashMap<>();
        payload.put("driverId", driverId);
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
