package ai.shreds.infrastructure.mappers;

import ai.shreds.domain.entities.DomainOutboxEventJpaEntity;
import ai.shreds.domain.events.*;
import ai.shreds.shared.dtos.SharedTripEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfrastructureEventMapper {

    private final ObjectMapper objectMapper;

    public SharedTripEventDTO toKafkaEvent(DomainDomainEvent domainEvent) {
        if (domainEvent == null) {
            return null;
        }

        try {
            SharedTripEventDTO kafkaEvent = new SharedTripEventDTO();
            kafkaEvent.setEventType(domainEvent.getEventType());
            kafkaEvent.setTripId(domainEvent.getAggregateId());
            kafkaEvent.setTimestamp(domainEvent.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            kafkaEvent.setPayload(convertPayloadToMap(domainEvent.getPayload()));
            kafkaEvent.setSource("trip-request-matching-shred");

            return kafkaEvent;

        } catch (Exception e) {
            log.error("Error mapping domain event to Kafka event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map domain event to Kafka event", e);
        }
    }

    public DomainOutboxEventJpaEntity toOutboxEntity(DomainDomainEvent domainEvent) {
        if (domainEvent == null) {
            return null;
        }

        try {
            DomainOutboxEventJpaEntity outboxEntity = new DomainOutboxEventJpaEntity();
            outboxEntity.setAggregateType("Trip");
            outboxEntity.setAggregateId(UUID.fromString(domainEvent.getAggregateId()));
            outboxEntity.setEventType(domainEvent.getEventType());
            outboxEntity.setEventPayload(serializePayload(domainEvent.getPayload()));
            outboxEntity.setPublished(false);
            outboxEntity.setCreatedAt(OffsetDateTime.of(domainEvent.getOccurredAt(), ZoneOffset.UTC));

            return outboxEntity;

        } catch (Exception e) {
            log.error("Error mapping domain event to outbox entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map domain event to outbox entity", e);
        }
    }

    public DomainDomainEvent fromOutboxEntity(DomainOutboxEventJpaEntity outboxEntity) {
        if (outboxEntity == null) {
            return null;
        }

        try {
            String eventType = outboxEntity.getEventType();
            String aggregateId = outboxEntity.getAggregateId().toString();
            LocalDateTime occurredAt = outboxEntity.getCreatedAt().toLocalDateTime();
            Map<String, Object> payload = deserializePayload(outboxEntity.getEventPayload());

            // Create specific domain event based on event type
            switch (eventType) {
                case "TRIP_REQUESTED":
                    return createTripRequestedEvent(aggregateId, occurredAt, payload);
                case "DRIVER_MATCHED":
                    return createDriverMatchedEvent(aggregateId, occurredAt, payload);
                case "MATCH_TIMEOUT":
                    return createMatchTimeoutEvent(aggregateId, occurredAt, payload);
                case "TRIP_CANCELLED":
                    return createTripCancelledEvent(aggregateId, occurredAt, payload);
                default:
                    log.warn("Unknown event type: {}, creating generic event", eventType);
                    return createGenericEvent(eventType, aggregateId, occurredAt, payload);
            }

        } catch (Exception e) {
            log.error("Error mapping outbox entity to domain event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map outbox entity to domain event", e);
        }
    }

    private DomainTripRequestedEvent createTripRequestedEvent(String aggregateId, LocalDateTime occurredAt, Map<String, Object> payload) {
        // Extract specific fields from payload for TripRequestedEvent
        String riderId = (String) payload.get("riderId");
        // Note: In a real implementation, you'd properly reconstruct the location values
        // For now, we'll create a minimal event
        return new DomainTripRequestedEvent(aggregateId, riderId, null, null, occurredAt);
    }

    private DomainDriverMatchedEvent createDriverMatchedEvent(String aggregateId, LocalDateTime occurredAt, Map<String, Object> payload) {
        String driverId = (String) payload.get("driverId");
        return new DomainDriverMatchedEvent(aggregateId, driverId, occurredAt);
    }

    private DomainMatchTimeoutEvent createMatchTimeoutEvent(String aggregateId, LocalDateTime occurredAt, Map<String, Object> payload) {
        String reason = (String) payload.get("reason");
        return new DomainMatchTimeoutEvent(aggregateId, reason, occurredAt);
    }

    private DomainTripCancelledEvent createTripCancelledEvent(String aggregateId, LocalDateTime occurredAt, Map<String, Object> payload) {
        return new DomainTripCancelledEvent(aggregateId, occurredAt);
    }

    private DomainDomainEvent createGenericEvent(String eventType, String aggregateId, LocalDateTime occurredAt, Map<String, Object> payload) {
        // Generic event implementation for unknown types
        return new DomainDomainEvent() {
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
                return payload;
            }
        };
    }

    private Map<String, Object> convertPayloadToMap(Map<String, Object> payload) {
        if (payload == null) {
            return new HashMap<>();
        }
        return new HashMap<>(payload);
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> deserializePayload(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize payload from JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}