package ai.shreds.domain.events;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DomainMatchTimeoutEvent implements DomainDomainEvent {
    private final String aggregateId;
    private final String eventType = "MATCH_TIMEOUT";
    private final LocalDateTime occurredAt;
    private final Map<String, Object> payload;

    public DomainMatchTimeoutEvent(String tripId, String reason, LocalDateTime timeoutAt) {
        this.aggregateId = tripId;
        this.occurredAt = timeoutAt;
        this.payload = new HashMap<>();
        this.payload.put("reason", reason);
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
