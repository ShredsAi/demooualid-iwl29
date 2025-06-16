package ai.shreds.infrastructure.mappers;

import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.entities.DomainTripJpaEntity;
import ai.shreds.domain.entities.DomainTripTimelineEntity;
import ai.shreds.domain.entities.DomainTripTimelineJpaEntity;
import ai.shreds.domain.entities.DomainTripEventJpaEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.value_objects.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfrastructureTripEntityMapper {

    private final ObjectMapper objectMapper;

    public DomainTripEntity toDomainEntity(DomainTripJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        try {
            // Use the static factory method from DomainTripEntity
            return DomainTripEntity.fromJpaEntity(jpaEntity);
        } catch (Exception e) {
            log.error("Error mapping JPA entity to domain entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map JPA entity to domain entity", e);
        }
    }

    public DomainTripJpaEntity toJpaEntity(DomainTripEntity domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        try {
            // Use the instance method from DomainTripEntity
            return domainEntity.toJpaEntity();
        } catch (Exception e) {
            log.error("Error mapping domain entity to JPA entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map domain entity to JPA entity", e);
        }
    }

    public DomainTripTimelineEntity toDomainTimeline(DomainTripTimelineJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        try {
            DomainTripIdValue tripId = new DomainTripIdValue(jpaEntity.getTripId().toString());
            
            // Create timeline with just tripId - constructor only accepts tripId parameter
            DomainTripTimelineEntity timeline = new DomainTripTimelineEntity(tripId);
            
            // Add events one by one using addEvent method
            List<DomainTripEventValue> events = jpaEntity.getEvents().stream()
                    .map(this::toDomainEvent)
                    .collect(Collectors.toList());
            
            for (DomainTripEventValue event : events) {
                timeline.addEvent(event);
            }

            return timeline;

        } catch (Exception e) {
            log.error("Error mapping timeline JPA entity to domain entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map timeline JPA entity to domain entity", e);
        }
    }

    public DomainTripTimelineJpaEntity toJpaTimeline(DomainTripTimelineEntity domainEntity) {
        if (domainEntity == null) {
            return null;
        }

        try {
            DomainTripTimelineJpaEntity jpaEntity = new DomainTripTimelineJpaEntity();
            jpaEntity.setTimelineId(UUID.fromString(domainEntity.getTripId().getValue()));
            jpaEntity.setTripId(UUID.fromString(domainEntity.getTripId().getValue()));
            jpaEntity.setCreatedAt(OffsetDateTime.now());

            List<DomainTripEventJpaEntity> eventEntities = domainEntity.getEvents().stream()
                    .map(event -> toJpaEvent(event, jpaEntity.getTimelineId()))
                    .collect(Collectors.toList());
            
            jpaEntity.setEvents(eventEntities);

            return jpaEntity;

        } catch (Exception e) {
            log.error("Error mapping timeline domain entity to JPA entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map timeline domain entity to JPA entity", e);
        }
    }

    public DomainTripEventValue toDomainEvent(DomainTripEventJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        try {
            DomainTripEventTypeEnum eventType = DomainTripEventTypeEnum.valueOf(jpaEntity.getEventType());
            LocalDateTime timestamp = jpaEntity.getEventTimestamp().toLocalDateTime();
            Map<String, String> data = mapMetadata(jpaEntity.getPayload());
            String source = "trip-request-matching-shred"; // Default source

            return new DomainTripEventValue(eventType, timestamp, data, source);

        } catch (Exception e) {
            log.error("Error mapping event JPA entity to domain value: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map event JPA entity to domain value", e);
        }
    }

    public DomainTripEventJpaEntity toJpaEvent(DomainTripEventValue domainValue, UUID timelineId) {
        if (domainValue == null) {
            return null;
        }

        try {
            DomainTripEventJpaEntity jpaEntity = new DomainTripEventJpaEntity();
            jpaEntity.setTimelineId(timelineId);
            jpaEntity.setEventType(domainValue.getEventType().name());
            jpaEntity.setEventTimestamp(OffsetDateTime.of(domainValue.getTimestamp(), ZoneOffset.UTC));
            jpaEntity.setPayload(mapMetadataToJson(domainValue.getData()));

            return jpaEntity;

        } catch (Exception e) {
            log.error("Error mapping event domain value to JPA entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map event domain value to JPA entity", e);
        }
    }

    private Map<String, String> mapMetadata(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata JSON: {}", e.getMessage());
            return Map.of();
        }
    }

    private String mapMetadataToJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}