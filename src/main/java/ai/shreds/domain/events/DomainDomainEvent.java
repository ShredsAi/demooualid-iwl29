package ai.shreds.domain.events;

import java.time.LocalDateTime;
import java.util.Map;

public interface DomainDomainEvent {
    String getAggregateId();
    String getEventType();
    LocalDateTime getOccurredAt();
    Map<String, Object> getPayload();
}
