package ai.shreds.domain.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class DomainOutboxEventJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;
    
    @Column(name = "aggregate_type", length = 50, nullable = false)
    private String aggregateType;
    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    
    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;
    
    @Column(name = "event_payload", columnDefinition = "JSONB", nullable = false)
    private String eventPayload;
    
    @Column(name = "published", nullable = false)
    private Boolean published = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
    
    // Constructors
    public DomainOutboxEventJpaEntity() {}
    
    public DomainOutboxEventJpaEntity(String aggregateType, UUID aggregateId, String eventType, String eventPayload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
        this.published = false;
    }
    
    // Getters and Setters
    public Long getOutboxId() {
        return outboxId;
    }
    
    public void setOutboxId(Long outboxId) {
        this.outboxId = outboxId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }
    
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getEventPayload() {
        return eventPayload;
    }
    
    public void setEventPayload(String eventPayload) {
        this.eventPayload = eventPayload;
    }
    
    public Boolean getPublished() {
        return published;
    }
    
    public void setPublished(Boolean published) {
        this.published = published;
        if (published && publishedAt == null) {
            this.publishedAt = OffsetDateTime.now();
        }
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}