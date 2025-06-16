package ai.shreds.domain.entities;

import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.domain.exceptions.DomainInvalidStateTransitionException;
import ai.shreds.domain.value_objects.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class DomainTripEntity {
    private final DomainTripIdValue id;
    private DomainTripStatusEnum status;
    private final DomainParticipantValue rider;
    private DomainParticipantValue driver;
    private final DomainLocationValue pickupLocation;
    private final DomainLocationValue dropoffLocation;
    private final LocalDateTime requestedAt;
    private final LocalDateTime scheduledFor;
    private final DomainMoneyValue estimatedFare;
    private DomainMoneyValue finalFare;
    private DomainCancellationInfoValue cancellationInfo;
    private final Map<String, String> metadata;
    private Long version;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DomainTripEntity(DomainTripIdValue id, DomainTripStatusEnum status, DomainParticipantValue rider,
                           DomainLocationValue pickupLocation, DomainLocationValue dropoffLocation,
                           LocalDateTime requestedAt, LocalDateTime scheduledFor,
                           DomainMoneyValue estimatedFare, Map<String, String> metadata) {
        this.id = id;
        this.status = status;
        this.rider = rider;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.requestedAt = requestedAt;
        this.scheduledFor = scheduledFor;
        this.estimatedFare = estimatedFare;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.version = 0L;
        validateEntity();
    }

    public void updateStatus(DomainTripStatusEnum newStatus) {
        if (!validateStateTransition(newStatus)) {
            throw new DomainInvalidStateTransitionException(this.status, newStatus);
        }
        this.status = newStatus;
    }

    public void assignDriver(DomainParticipantValue driver) {
        if (this.status != DomainTripStatusEnum.MATCHING) {
            throw new DomainInvalidStateTransitionException(this.status, DomainTripStatusEnum.MATCHED);
        }
        this.driver = driver;
        this.status = DomainTripStatusEnum.MATCHED;
    }

    public void cancel(DomainCancellationInfoValue cancellationInfo) {
        if (!canBeCancelled()) {
            throw new DomainBusinessRuleViolationException("Trip cannot be cancelled in current state", 
                Map.of("currentStatus", this.status));
        }
        this.cancellationInfo = cancellationInfo;
        this.status = DomainTripStatusEnum.CANCELLED;
    }

    public void complete(DomainMoneyValue finalFare) {
        if (this.status != DomainTripStatusEnum.IN_PROGRESS) {
            throw new DomainInvalidStateTransitionException(this.status, DomainTripStatusEnum.COMPLETED);
        }
        validateFinalFare(finalFare);
        this.finalFare = finalFare;
        this.status = DomainTripStatusEnum.COMPLETED;
    }

    public boolean isScheduled() {
        return scheduledFor != null && scheduledFor.isAfter(requestedAt);
    }

    public boolean canBeCancelled() {
        return status != DomainTripStatusEnum.COMPLETED && 
               status != DomainTripStatusEnum.CANCELLED &&
               status != DomainTripStatusEnum.MATCH_FAILED;
    }

    public DomainTripJpaEntity toJpaEntity() {
        DomainTripJpaEntity jpaEntity = new DomainTripJpaEntity();
        
        // Basic fields
        jpaEntity.setTripId(UUID.fromString(id.getValue()));
        jpaEntity.setStatus(status.name());
        jpaEntity.setRiderId(UUID.fromString(rider.getId()));
        
        if (driver != null) {
            jpaEntity.setDriverId(UUID.fromString(driver.getId()));
        }
        
        // Location fields
        jpaEntity.setPickupLatitude(pickupLocation.getLatitude());
        jpaEntity.setPickupLongitude(pickupLocation.getLongitude());
        jpaEntity.setPickupAddress(pickupLocation.getAddress());
        jpaEntity.setDropoffLatitude(dropoffLocation.getLatitude());
        jpaEntity.setDropoffLongitude(dropoffLocation.getLongitude());
        jpaEntity.setDropoffAddress(dropoffLocation.getAddress());
        
        // Datetime fields
        jpaEntity.setRequestedAt(requestedAt.atOffset(ZoneOffset.UTC));
        if (scheduledFor != null) {
            jpaEntity.setScheduledFor(scheduledFor.atOffset(ZoneOffset.UTC));
        }
        
        // Money fields
        jpaEntity.setEstimatedFareAmount(estimatedFare.getAmount());
        jpaEntity.setEstimatedFareCurrency(estimatedFare.getCurrency());
        
        if (finalFare != null) {
            jpaEntity.setFinalFareAmount(finalFare.getAmount());
            jpaEntity.setFinalFareCurrency(finalFare.getCurrency());
        }
        
        // Cancellation info
        if (cancellationInfo != null) {
            jpaEntity.setCancellationReason(cancellationInfo.getReason().name());
            jpaEntity.setCancelledBy(cancellationInfo.getCancelledBy());
            jpaEntity.setCancelledAt(cancellationInfo.getCancelledAt().atOffset(ZoneOffset.UTC));
            
            if (cancellationInfo.getRefundAmount() != null) {
                jpaEntity.setRefundAmount(cancellationInfo.getRefundAmount().getAmount());
                jpaEntity.setRefundCurrency(cancellationInfo.getRefundAmount().getCurrency());
            }
        }
        
        // Metadata as JSON
        try {
            jpaEntity.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new DomainBusinessRuleViolationException("Failed to serialize metadata", 
                Map.of("error", e.getMessage()));
        }
        
        jpaEntity.setVersion(version.intValue());
        
        return jpaEntity;
    }

    public static DomainTripEntity fromJpaEntity(DomainTripJpaEntity entity) {
        // Parse metadata from JSON
        Map<String, String> metadata = new HashMap<>();
        if (entity.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(), new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                throw new DomainBusinessRuleViolationException("Failed to deserialize metadata", 
                    Map.of("error", e.getMessage()));
            }
        }
        
        // Build value objects
        DomainTripIdValue id = new DomainTripIdValue(entity.getTripId().toString());
        DomainTripStatusEnum status = DomainTripStatusEnum.valueOf(entity.getStatus());
        
        // For now, we'll create basic participant values since we don't have full rider/driver info in JPA entity
        // In a real scenario, these would be fetched from their respective services
        DomainParticipantValue rider = new DomainParticipantValue(
            entity.getRiderId().toString(), 
            "Rider", // Default name - would be fetched from service
            "+1234567890", // Default phone - would be fetched from service
            java.math.BigDecimal.valueOf(4.5) // Default rating - would be fetched from service
        );
        
        DomainParticipantValue driver = null;
        if (entity.getDriverId() != null) {
            driver = new DomainParticipantValue(
                entity.getDriverId().toString(),
                "Driver", // Default name - would be fetched from service
                "+1234567890", // Default phone - would be fetched from service
                java.math.BigDecimal.valueOf(4.5) // Default rating - would be fetched from service
            );
        }
        
        DomainLocationValue pickupLocation = new DomainLocationValue(
            entity.getPickupLatitude(),
            entity.getPickupLongitude(),
            entity.getPickupAddress(),
            "City", // Default city - not stored separately in JPA
            "12345" // Default postal code - not stored separately in JPA
        );
        
        DomainLocationValue dropoffLocation = new DomainLocationValue(
            entity.getDropoffLatitude(),
            entity.getDropoffLongitude(),
            entity.getDropoffAddress(),
            "City", // Default city - not stored separately in JPA
            "12345" // Default postal code - not stored separately in JPA
        );
        
        DomainMoneyValue estimatedFare = new DomainMoneyValue(
            entity.getEstimatedFareAmount(),
            entity.getEstimatedFareCurrency()
        );
        
        LocalDateTime requestedAt = entity.getRequestedAt().toLocalDateTime();
        LocalDateTime scheduledFor = entity.getScheduledFor() != null ? 
            entity.getScheduledFor().toLocalDateTime() : null;
        
        // Create domain entity
        DomainTripEntity domainEntity = new DomainTripEntity(
            id, status, rider, pickupLocation, dropoffLocation,
            requestedAt, scheduledFor, estimatedFare, metadata
        );
        
        // Set optional fields
        if (driver != null) {
            domainEntity.driver = driver;
        }
        
        if (entity.getFinalFareAmount() != null) {
            domainEntity.finalFare = new DomainMoneyValue(
                entity.getFinalFareAmount(),
                entity.getFinalFareCurrency()
            );
        }
        
        if (entity.getCancellationReason() != null) {
            DomainMoneyValue refundAmount = null;
            if (entity.getRefundAmount() != null) {
                refundAmount = new DomainMoneyValue(
                    entity.getRefundAmount(),
                    entity.getRefundCurrency()
                );
            }
            
            domainEntity.cancellationInfo = new DomainCancellationInfoValue(
                DomainCancellationReasonEnum.valueOf(entity.getCancellationReason()),
                entity.getCancelledBy(),
                entity.getCancelledAt().toLocalDateTime(),
                refundAmount
            );
        }
        
        domainEntity.version = entity.getVersion().longValue();
        
        return domainEntity;
    }

    private void validateEntity() {
        if (id == null) {
            throw new DomainBusinessRuleViolationException("Trip ID cannot be null", null);
        }
        if (status == null) {
            throw new DomainBusinessRuleViolationException("Trip status cannot be null", null);
        }
        if (rider == null) {
            throw new DomainBusinessRuleViolationException("Rider cannot be null", null);
        }
        if (pickupLocation == null || dropoffLocation == null) {
            throw new DomainBusinessRuleViolationException("Pickup and dropoff locations cannot be null", null);
        }
        if (requestedAt == null) {
            throw new DomainBusinessRuleViolationException("Request timestamp cannot be null", null);
        }
        if (estimatedFare == null) {
            throw new DomainBusinessRuleViolationException("Estimated fare cannot be null", null);
        }
        validateLocations();
        validateScheduledTime();
    }

    private void validateLocations() {
        if (pickupLocation.equals(dropoffLocation)) {
            throw new DomainBusinessRuleViolationException("Pickup and dropoff locations cannot be the same", null);
        }
    }

    private void validateScheduledTime() {
        if (scheduledFor != null && scheduledFor.isBefore(requestedAt.plusMinutes(15))) {
            throw new DomainBusinessRuleViolationException(
                "Scheduled time must be at least 15 minutes after request time",
                Map.of("requestedAt", requestedAt, "scheduledFor", scheduledFor));
        }
    }

    private void validateFinalFare(DomainMoneyValue finalFare) {
        if (finalFare == null) {
            throw new DomainBusinessRuleViolationException("Final fare cannot be null", null);
        }
        if (!finalFare.getCurrency().equals(estimatedFare.getCurrency())) {
            throw new DomainBusinessRuleViolationException("Final fare currency must match estimated fare currency",
                Map.of("estimatedCurrency", estimatedFare.getCurrency(), "finalCurrency", finalFare.getCurrency()));
        }
        if (finalFare.getAmount().compareTo(estimatedFare.getAmount().multiply(new java.math.BigDecimal("2.0"))) > 0) {
            throw new DomainBusinessRuleViolationException(
                "Final fare cannot be more than 200% of estimated fare without approval",
                Map.of("estimatedFare", estimatedFare, "finalFare", finalFare));
        }
    }

    public boolean validateStateTransition(DomainTripStatusEnum newStatus) {
        if (newStatus == null) return false;
        if (this.status == newStatus) return true;

        switch (this.status) {
            case REQUESTED:
                return newStatus == DomainTripStatusEnum.MATCHING || 
                       newStatus == DomainTripStatusEnum.CANCELLED;
            case MATCHING:
                return newStatus == DomainTripStatusEnum.MATCHED || 
                       newStatus == DomainTripStatusEnum.CANCELLED || 
                       newStatus == DomainTripStatusEnum.MATCH_FAILED;
            case MATCHED:
                return newStatus == DomainTripStatusEnum.DRIVER_EN_ROUTE || 
                       newStatus == DomainTripStatusEnum.CANCELLED;
            case DRIVER_EN_ROUTE:
                return newStatus == DomainTripStatusEnum.ARRIVED || 
                       newStatus == DomainTripStatusEnum.CANCELLED;
            case ARRIVED:
                return newStatus == DomainTripStatusEnum.RIDER_PICKED_UP || 
                       newStatus == DomainTripStatusEnum.CANCELLED;
            case RIDER_PICKED_UP:
                return newStatus == DomainTripStatusEnum.IN_PROGRESS;
            case IN_PROGRESS:
                return newStatus == DomainTripStatusEnum.COMPLETED || 
                       newStatus == DomainTripStatusEnum.CANCELLED;
            default:
                return false;
        }
    }

    // Getters
    public DomainTripIdValue getId() { return id; }
    public DomainTripStatusEnum getStatus() { return status; }
    public DomainParticipantValue getRider() { return rider; }
    public DomainParticipantValue getDriver() { return driver; }
    public DomainLocationValue getPickupLocation() { return pickupLocation; }
    public DomainLocationValue getDropoffLocation() { return dropoffLocation; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public DomainMoneyValue getEstimatedFare() { return estimatedFare; }
    public DomainMoneyValue getFinalFare() { return finalFare; }
    public DomainCancellationInfoValue getCancellationInfo() { return cancellationInfo; }
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainTripEntity that = (DomainTripEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}