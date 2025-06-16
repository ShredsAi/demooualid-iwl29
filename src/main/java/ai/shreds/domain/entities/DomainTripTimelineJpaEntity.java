package ai.shreds.domain.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trip_timeline")
public class DomainTripTimelineJpaEntity {
    
    @Id
    @Column(name = "timeline_id", nullable = false)
    private UUID timelineId;
    
    @Column(name = "trip_id", nullable = false, unique = true)
    private UUID tripId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @OneToMany(mappedBy = "timelineId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("eventTimestamp ASC")
    private List<DomainTripEventJpaEntity> events = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (timelineId == null) {
            timelineId = UUID.randomUUID();
        }
    }
    
    // Constructors
    public DomainTripTimelineJpaEntity() {}
    
    public DomainTripTimelineJpaEntity(UUID tripId) {
        this.tripId = tripId;
        this.timelineId = tripId; // Same as trip_id for 1-to-1 mapping
    }
    
    // Getters and Setters
    public UUID getTimelineId() {
        return timelineId;
    }
    
    public void setTimelineId(UUID timelineId) {
        this.timelineId = timelineId;
    }
    
    public UUID getTripId() {
        return tripId;
    }
    
    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<DomainTripEventJpaEntity> getEvents() {
        return events;
    }
    
    public void setEvents(List<DomainTripEventJpaEntity> events) {
        this.events = events;
    }
    
    public void addEvent(DomainTripEventJpaEntity event) {
        events.add(event);
        event.setTimelineId(this.timelineId);
    }
}