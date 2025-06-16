package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainTripTimelineEntity;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.value_objects.DomainTripEventValue;
import java.util.List;

public interface DomainTimelineServiceInputPort {
    void addEvent(String tripId, DomainTripEventValue event);
    DomainTripTimelineEntity getTimeline(String tripId);
    List<DomainTripEventValue> getEventsByType(String tripId, DomainTripEventTypeEnum type);
}
