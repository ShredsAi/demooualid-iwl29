package ai.shreds.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_event")
public class DomainTripEventJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;
    
    @Column(name = "timeline_id", nullable = false)
    private UUID timelineId;
    
    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;
    
    @Column(name = "event_timestamp", nullable = false)
    private OffsetDateTime eventTimestamp;
    
    @Column(name = "payload", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;
    
    // Constructors
    public DomainTripEventJpaEntity() {}
    
    public DomainTripEventJpaEntity(UUID timelineId, String eventType, OffsetDateTime eventTimestamp, String payload) {
        this.timelineId = timelineId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.payload = payload;
    }
    
    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }
    
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
    
    public UUID getTimelineId() {
        return timelineId;
    }
    
    public void setTimelineId(UUID timelineId) {
        this.timelineId = timelineId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public OffsetDateTime getEventTimestamp() {
        return eventTimestamp;
    }
    
    public void setEventTimestamp(OffsetDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
}