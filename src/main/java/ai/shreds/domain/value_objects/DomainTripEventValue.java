package ai.shreds.domain.value_objects;

import ai.shreds.domain.entities.DomainTripEventJpaEntity;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class DomainTripEventValue {
    private final DomainTripEventTypeEnum eventType;
    private final LocalDateTime timestamp;
    private final Map<String, String> data;
    private final String source;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DomainTripEventValue(DomainTripEventTypeEnum eventType, LocalDateTime timestamp, 
                               Map<String, String> data, String source) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.source = source;
        validateTimestamp();
    }

    public DomainTripEventTypeEnum getEventType() {
        return eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getData() {
        return new HashMap<>(data);
    }

    public String getSource() {
        return source;
    }

    public DomainTripEventJpaEntity toJpaEntity() {
        DomainTripEventJpaEntity jpaEntity = new DomainTripEventJpaEntity();
        jpaEntity.setEventType(eventType.name());
        jpaEntity.setEventTimestamp(timestamp.atOffset(ZoneOffset.UTC));
        
        // Create payload JSON combining data and source
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        payload.put("source", source);
        
        try {
            jpaEntity.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new DomainBusinessRuleViolationException("Failed to serialize event payload", 
                Map.of("error", e.getMessage()));
        }
        
        return jpaEntity;
    }

    public static DomainTripEventValue fromJpaEntity(DomainTripEventJpaEntity entity) {
        DomainTripEventTypeEnum eventType = DomainTripEventTypeEnum.valueOf(entity.getEventType());
        LocalDateTime timestamp = entity.getEventTimestamp().toLocalDateTime();
        
        Map<String, String> data = new HashMap<>();
        String source = "system"; // default source
        
        if (entity.getPayload() != null) {
            try {
                Map<String, Object> payload = objectMapper.readValue(entity.getPayload(), 
                    new TypeReference<Map<String, Object>>() {});
                
                if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> dataMap = (Map<String, String>) payload.get("data");
                    data = dataMap;
                }
                
                if (payload.containsKey("source")) {
                    source = payload.get("source").toString();
                }
            } catch (JsonProcessingException e) {
                throw new DomainBusinessRuleViolationException("Failed to deserialize event payload", 
                    Map.of("error", e.getMessage()));
            }
        }
        
        return new DomainTripEventValue(eventType, timestamp, data, source);
    }

    public void validateTimestamp() {
        if (eventType == null) {
            throw new DomainBusinessRuleViolationException("Event type cannot be null", null);
        }
        if (timestamp == null) {
            throw new DomainBusinessRuleViolationException("Event timestamp cannot be null", null);
        }
        if (timestamp.isAfter(LocalDateTime.now())) {
            Map<String, Object> details = new HashMap<>();
            details.put("eventTimestamp", timestamp);
            details.put("currentTime", LocalDateTime.now());
            throw new DomainBusinessRuleViolationException("Event timestamp cannot be in the future", details);
        }
        if (source == null || source.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Event source cannot be null or empty", null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainTripEventValue that = (DomainTripEventValue) o;
        return eventType == that.eventType &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(data, that.data) &&
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, timestamp, data, source);
    }
}