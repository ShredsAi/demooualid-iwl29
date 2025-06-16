package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainCreateTripCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.domain.value_objects.DomainCancellationInfoValue;
import ai.shreds.domain.value_objects.DomainLocationValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.domain.value_objects.DomainParticipantValue;
import ai.shreds.domain.value_objects.DomainTripIdValue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DomainTripFactory {

    public DomainTripEntity createTrip(DomainCreateTripCommand command) {
        DomainTripIdValue id = generateTripId();
        DomainParticipantValue rider = new DomainParticipantValue(
            command.getRiderId(),
            command.getRiderName(),
            command.getRiderPhone(),
            command.getRiderRating()
        );
        validatePickupDropoffDifferent(command.getPickupLocation(), command.getDropoffLocation());
        LocalDateTime now = LocalDateTime.now();
        validateScheduledTime(now, command.getScheduledFor());
        return new DomainTripEntity(
            id,
            DomainTripStatusEnum.REQUESTED,
            rider,
            command.getPickupLocation(),
            command.getDropoffLocation(),
            now,
            command.getScheduledFor(),
            command.getEstimatedFare(),
            command.getMetadata()
        );
    }

    public void validatePickupDropoffDifferent(DomainLocationValue pickup, DomainLocationValue dropoff) {
        if (pickup.equals(dropoff)) {
            throw new DomainBusinessRuleViolationException(
                "Pickup and dropoff locations cannot be the same", null
            );
        }
    }

    public void validateScheduledTime(LocalDateTime requestedAt, LocalDateTime scheduledFor) {
        if (scheduledFor != null && scheduledFor.isBefore(requestedAt.plusMinutes(15))) {
            throw new DomainBusinessRuleViolationException(
                "Scheduled time must be at least 15 minutes after request",
                Map.of("requestedAt", requestedAt, "scheduledFor", scheduledFor)
            );
        }
    }

    public DomainTripIdValue generateTripId() {
        return new DomainTripIdValue(UUID.randomUUID().toString());
    }
}