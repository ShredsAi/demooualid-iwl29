package ai.shreds.domain.ports;

import ai.shreds.domain.events.DomainDomainEvent;
import java.util.List;

public interface DomainEventStoreOutputPort {
    void saveEvent(DomainDomainEvent event);
    List<DomainDomainEvent> getUnpublishedEvents();
    void markAsPublished(long eventId);
}
