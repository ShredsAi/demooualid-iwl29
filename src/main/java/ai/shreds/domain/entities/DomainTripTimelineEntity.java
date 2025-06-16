package ai.shreds.domain.entities;

import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.domain.value_objects.DomainTripEventValue;
import ai.shreds.domain.value_objects.DomainTripIdValue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DomainTripTimelineEntity {
    private final DomainTripIdValue tripId;
    private final List<DomainTripEventValue> events;

    public DomainTripTimelineEntity(DomainTripIdValue tripId) {
        this.tripId = tripId;
        this.events = new ArrayList<>();
        validateEntity();
    }

    public void addEvent(DomainTripEventValue event) {
        validateChronologicalOrder(event);
        events.add(event);
    }

    public List<DomainTripEventValue> getEventsByType(DomainTripEventTypeEnum type) {
        return events.stream()
                .filter(event -> event.getEventType() == type)
                .collect(Collectors.toList());
    }

    public DomainTripEventValue getLatestEvent() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1);
    }

    public List<DomainTripEventValue> getEventsSince(LocalDateTime timestamp) {
        return events.stream()
                .filter(event -> event.getTimestamp().isAfter(timestamp))
                .collect(Collectors.toList());
    }

    public boolean hasEventType(DomainTripEventTypeEnum type) {
        return events.stream().anyMatch(event -> event.getEventType() == type);
    }

    public boolean validateChronologicalOrder(DomainTripEventValue newEvent) {
        if (events.isEmpty()) {
            return true;
        }
        DomainTripEventValue latestEvent = getLatestEvent();
        if (newEvent.getTimestamp().isBefore(latestEvent.getTimestamp())) {
            throw new DomainBusinessRuleViolationException(
                "New event timestamp cannot be before the latest event",
                Map.of("latestEventTime", latestEvent.getTimestamp(),
                       "newEventTime", newEvent.getTimestamp()));
        }
        return true;
    }

    public DomainTripTimelineJpaEntity toJpaEntity() {
        DomainTripTimelineJpaEntity jpaEntity = new DomainTripTimelineJpaEntity();
        jpaEntity.setTimelineId(UUID.fromString(tripId.getValue()));
        jpaEntity.setTripId(UUID.fromString(tripId.getValue()));
        jpaEntity.setCreatedAt(OffsetDateTime.now());
        
        List<DomainTripEventJpaEntity> jpaEvents = events.stream()
                .map(event -> event.toJpaEntity())
                .collect(Collectors.toList());
        jpaEntity.setEvents(jpaEvents);
        
        return jpaEntity;
    }

    public static DomainTripTimelineEntity fromJpaEntity(DomainTripTimelineJpaEntity entity) {
        DomainTripIdValue tripId = new DomainTripIdValue(entity.getTripId().toString());
        DomainTripTimelineEntity timeline = new DomainTripTimelineEntity(tripId);
        
        List<DomainTripEventValue> domainEvents = entity.getEvents().stream()
                .map(jpaEvent -> DomainTripEventValue.fromJpaEntity(jpaEvent))
                .collect(Collectors.toList());
        
        domainEvents.forEach(timeline::addEvent);
        
        return timeline;
    }

    private void validateEntity() {
        if (tripId == null) {
            throw new DomainBusinessRuleViolationException("Trip ID cannot be null", null);
        }
    }

    public DomainTripIdValue getTripId() {
        return tripId;
    }

    public List<DomainTripEventValue> getEvents() {
        return new ArrayList<>(events);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainTripTimelineEntity that = (DomainTripTimelineEntity) o;
        return Objects.equals(tripId, that.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId);
    }
}