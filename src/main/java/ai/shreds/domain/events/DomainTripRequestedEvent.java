package ai.shreds.domain.events;

import ai.shreds.domain.events.DomainDomainEvent;
import ai.shreds.domain.value_objects.DomainLocationValue;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DomainTripRequestedEvent implements DomainDomainEvent {
    private final String aggregateId;
    private final String eventType = "TRIP_REQUESTED";
    private final LocalDateTime occurredAt;
    private final Map<String, Object> payload;

    public DomainTripRequestedEvent(String tripId, String riderId,
                                    DomainLocationValue pickupLocation,
                                    DomainLocationValue dropoffLocation,
                                    LocalDateTime requestedAt) {
        this.aggregateId = tripId;
        this.occurredAt = requestedAt;
        this.payload = new HashMap<>();
        payload.put("riderId", riderId);
        payload.put("pickupLocation", pickupLocation);
        payload.put("dropoffLocation", dropoffLocation);
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
