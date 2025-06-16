package ai.shreds.domain.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip")
public class DomainTripJpaEntity {
    
    @Id
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;
    
    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private String status;
    
    @Column(name = "rider_id", nullable = false)
    private UUID riderId;
    
    @Column(name = "driver_id")
    private UUID driverId;
    
    @Column(name = "pickup_latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal pickupLatitude;
    
    @Column(name = "pickup_longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal pickupLongitude;
    
    @Column(name = "pickup_address", length = 255, nullable = false)
    private String pickupAddress;
    
    @Column(name = "dropoff_latitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal dropoffLatitude;
    
    @Column(name = "dropoff_longitude", precision = 9, scale = 6, nullable = false)
    private BigDecimal dropoffLongitude;
    
    @Column(name = "dropoff_address", length = 255, nullable = false)
    private String dropoffAddress;
    
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;
    
    @Column(name = "scheduled_for")
    private OffsetDateTime scheduledFor;
    
    @Column(name = "estimated_fare_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal estimatedFareAmount;
    
    @Column(name = "estimated_fare_currency", length = 3, nullable = false)
    private String estimatedFareCurrency;
    
    @Column(name = "final_fare_amount", precision = 10, scale = 2)
    private BigDecimal finalFareAmount;
    
    @Column(name = "final_fare_currency", length = 3)
    private String finalFareCurrency;
    
    @Column(name = "cancellation_reason", length = 40)
    private String cancellationReason;
    
    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;
    
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;
    
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "refund_currency", length = 3)
    private String refundCurrency;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (tripId == null) {
            tripId = UUID.randomUUID();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
    
    // Constructors
    public DomainTripJpaEntity() {}
    
    // Getters and Setters
    public UUID getTripId() {
        return tripId;
    }
    
    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public UUID getRiderId() {
        return riderId;
    }
    
    public void setRiderId(UUID riderId) {
        this.riderId = riderId;
    }
    
    public UUID getDriverId() {
        return driverId;
    }
    
    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }
    
    public BigDecimal getPickupLatitude() {
        return pickupLatitude;
    }
    
    public void setPickupLatitude(BigDecimal pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }
    
    public BigDecimal getPickupLongitude() {
        return pickupLongitude;
    }
    
    public void setPickupLongitude(BigDecimal pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }
    
    public String getPickupAddress() {
        return pickupAddress;
    }
    
    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
    
    public BigDecimal getDropoffLatitude() {
        return dropoffLatitude;
    }
    
    public void setDropoffLatitude(BigDecimal dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }
    
    public BigDecimal getDropoffLongitude() {
        return dropoffLongitude;
    }
    
    public void setDropoffLongitude(BigDecimal dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }
    
    public String getDropoffAddress() {
        return dropoffAddress;
    }
    
    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }
    
    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public OffsetDateTime getScheduledFor() {
        return scheduledFor;
    }
    
    public void setScheduledFor(OffsetDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }
    
    public BigDecimal getEstimatedFareAmount() {
        return estimatedFareAmount;
    }
    
    public void setEstimatedFareAmount(BigDecimal estimatedFareAmount) {
        this.estimatedFareAmount = estimatedFareAmount;
    }
    
    public String getEstimatedFareCurrency() {
        return estimatedFareCurrency;
    }
    
    public void setEstimatedFareCurrency(String estimatedFareCurrency) {
        this.estimatedFareCurrency = estimatedFareCurrency;
    }
    
    public BigDecimal getFinalFareAmount() {
        return finalFareAmount;
    }
    
    public void setFinalFareAmount(BigDecimal finalFareAmount) {
        this.finalFareAmount = finalFareAmount;
    }
    
    public String getFinalFareCurrency() {
        return finalFareCurrency;
    }
    
    public void setFinalFareCurrency(String finalFareCurrency) {
        this.finalFareCurrency = finalFareCurrency;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public String getCancelledBy() {
        return cancelledBy;
    }
    
    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
    
    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getRefundCurrency() {
        return refundCurrency;
    }
    
    public void setRefundCurrency(String refundCurrency) {
        this.refundCurrency = refundCurrency;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}