package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainTripTimelineEntity;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.value_objects.DomainTripEventValue;
import java.util.List;
import java.util.Optional;

public interface DomainTimelineRepositoryOutputPort {
    DomainTripTimelineEntity save(DomainTripTimelineEntity timeline);
    Optional<DomainTripTimelineEntity> findByTripId(String tripId);
    void addEvent(String tripId, DomainTripEventValue event);
    List<DomainTripEventValue> getEventsByType(String tripId, DomainTripEventTypeEnum eventType);
}
