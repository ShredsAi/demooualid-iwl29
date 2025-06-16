package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripTimelineEntity;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.ports.DomainTimelineRepositoryOutputPort;
import ai.shreds.domain.value_objects.DomainTripEventValue;
import ai.shreds.infrastructure.exceptions.InfrastructureRepositoryException;
import ai.shreds.infrastructure.mappers.InfrastructureTripEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InfrastructureTimelineRepositoryImpl implements DomainTimelineRepositoryOutputPort {

    private final InfrastructureTimelineJpaRepository timelineJpaRepository;
    private final InfrastructureTripEventJpaRepository tripEventJpaRepository;
    private final InfrastructureTripEntityMapper entityMapper;

    @Override
    public DomainTripTimelineEntity save(DomainTripTimelineEntity timeline) {
        try {
            log.debug("Saving timeline for trip ID: {}", timeline.getTripId().getValue());
            var jpaEntity = entityMapper.toJpaTimeline(timeline);
            var savedJpaEntity = timelineJpaRepository.save(jpaEntity);
            var savedDomainEntity = entityMapper.toDomainTimeline(savedJpaEntity);
            log.debug("Successfully saved timeline for trip ID: {}", savedDomainEntity.getTripId().getValue());
            return savedDomainEntity;
        } catch (Exception e) {
            log.error("Failed to save timeline for trip ID: {}", timeline.getTripId().getValue(), e);
            throw new InfrastructureRepositoryException("save", "TripTimeline", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DomainTripTimelineEntity> findByTripId(String tripId) {
        try {
            log.debug("Finding timeline by trip ID: {}", tripId);
            UUID uuid = UUID.fromString(tripId);
            Optional<DomainTripTimelineEntity> result = timelineJpaRepository.findByTripId(uuid)
                    .map(entityMapper::toDomainTimeline);
            log.debug("Timeline for trip ID {} {}", tripId, result.isPresent() ? "found" : "not found");
            return result;
        } catch (Exception e) {
            log.error("Failed to find timeline by trip ID: {}", tripId, e);
            throw new InfrastructureRepositoryException("findByTripId", "TripTimeline", e);
        }
    }

    @Override
    public void addEvent(String tripId, DomainTripEventValue event) {
        try {
            log.debug("Adding event {} to trip ID: {}", event.getEventType(), tripId);
            
            // Find timeline by trip ID
            UUID tripUuid = UUID.fromString(tripId);
            var timelineJpa = timelineJpaRepository.findByTripId(tripUuid)
                    .orElseThrow(() -> new InfrastructureRepositoryException("addEvent", "TripTimeline",
                            new IllegalArgumentException("Timeline not found for trip: " + tripId)));
            
            // Convert and save event
            var eventJpa = entityMapper.toJpaEvent(event, timelineJpa.getTimelineId());
            tripEventJpaRepository.save(eventJpa);
            
            log.debug("Successfully added event {} to trip ID: {}", event.getEventType(), tripId);
        } catch (Exception e) {
            log.error("Failed to add event to trip ID: {}", tripId, e);
            throw new InfrastructureRepositoryException("addEvent", "TripEvent", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainTripEventValue> getEventsByType(String tripId, DomainTripEventTypeEnum eventType) {
        try {
            log.debug("Finding events of type {} for trip ID: {}", eventType, tripId);
            
            // Find timeline by trip ID
            UUID tripUuid = UUID.fromString(tripId);
            var timelineJpa = timelineJpaRepository.findByTripId(tripUuid)
                    .orElseThrow(() -> new InfrastructureRepositoryException("getEventsByType", "TripTimeline",
                            new IllegalArgumentException("Timeline not found for trip: " + tripId)));
            
            // Find events by type
            List<DomainTripEventValue> events = tripEventJpaRepository
                    .findByTimelineIdAndEventType(timelineJpa.getTimelineId(), eventType.name())
                    .stream()
                    .map(entityMapper::toDomainEvent)
                    .collect(Collectors.toList());
            
            log.debug("Found {} events of type {} for trip ID: {}", events.size(), eventType, tripId);
            return events;
        } catch (Exception e) {
            log.error("Failed to find events by type for trip ID: {}", tripId, e);
            throw new InfrastructureRepositoryException("getEventsByType", "TripEvent", e);
        }
    }
}