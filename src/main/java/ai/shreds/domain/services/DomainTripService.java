package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainAssignDriverCommand;
import ai.shreds.domain.commands.DomainCancelTripCommand;
import ai.shreds.domain.commands.DomainCreateTripCommand;
import ai.shreds.domain.commands.DomainMatchingTimeoutCommand;
import ai.shreds.domain.commands.DomainTimeoutCommand;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.entities.DomainTripTimelineEntity;
import ai.shreds.domain.enums.DomainCancellationReasonEnum;
import ai.shreds.domain.enums.DomainTripEventTypeEnum;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.events.DomainDriverMatchedEvent;
import ai.shreds.domain.events.DomainMatchTimeoutEvent;
import ai.shreds.domain.events.DomainTripCancelledEvent;
import ai.shreds.domain.events.DomainTripRequestedEvent;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.domain.exceptions.DomainEntityNotFoundException;
import ai.shreds.domain.exceptions.DomainInvalidStateTransitionException;
import ai.shreds.domain.ports.DomainEventStoreOutputPort;
import ai.shreds.domain.ports.DomainTimelineRepositoryOutputPort;
import ai.shreds.domain.ports.DomainTripRepositoryOutputPort;
import ai.shreds.domain.ports.DomainTripServiceInputPort;
import ai.shreds.domain.value_objects.DomainCancellationInfoValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.domain.value_objects.DomainParticipantValue;
import ai.shreds.domain.value_objects.DomainTripEventValue;
import ai.shreds.domain.value_objects.DomainTripIdValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DomainTripService implements DomainTripServiceInputPort {
    private final DomainTripRepositoryOutputPort tripRepository;
    private final DomainTimelineRepositoryOutputPort timelineRepository;
    private final DomainEventStoreOutputPort eventStore;
    private final DomainStateMachineService stateMachine;
    private final DomainTripFactory tripFactory;

    @Override
    public DomainTripEntity createTrip(DomainCreateTripCommand command) {
        DomainTripEntity trip = tripFactory.createTrip(command);
        tripRepository.save(trip);
        DomainTripTimelineEntity timeline = new DomainTripTimelineEntity(trip.getId());
        timelineRepository.save(timeline);
        timelineRepository.addEvent(trip.getId().getValue(),
            new DomainTripEventValue(
                DomainTripEventTypeEnum.REQUESTED,
                trip.getRequestedAt(),
                Collections.emptyMap(),
                "DomainTripService"
            )
        );
        DomainTripRequestedEvent domainEvent = new DomainTripRequestedEvent(
            trip.getId().getValue(),
            trip.getRider().getId(),
            trip.getPickupLocation(),
            trip.getDropoffLocation(),
            trip.getRequestedAt()
        );
        eventStore.saveEvent(domainEvent);
        return trip;
    }

    @Override
    public DomainTripEntity updateTripStatus(String tripId, DomainTripStatusEnum newStatus) {
        DomainTripEntity trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new DomainEntityNotFoundException("Trip", tripId));
        if (!stateMachine.validateTransition(trip.getStatus(), newStatus)) {
            throw new DomainInvalidStateTransitionException(trip.getStatus(), newStatus);
        }
        trip.updateStatus(newStatus);
        tripRepository.update(trip);
        return trip;
    }

    @Override
    public DomainTripEntity assignDriver(DomainAssignDriverCommand command) {
        DomainTripEntity trip = tripRepository.findById(command.getTripId())
            .orElseThrow(() -> new DomainEntityNotFoundException("Trip", command.getTripId()));
        if (!stateMachine.validateTransition(trip.getStatus(), DomainTripStatusEnum.MATCHED)) {
            throw new DomainInvalidStateTransitionException(trip.getStatus(), DomainTripStatusEnum.MATCHED);
        }
        DomainParticipantValue driver = new DomainParticipantValue(
            command.getDriverId(),
            command.getDriverName(),
            command.getDriverPhone(),
            command.getDriverRating()
        );
        trip.assignDriver(driver);
        tripRepository.update(trip);
        timelineRepository.addEvent(trip.getId().getValue(),
            new DomainTripEventValue(
                DomainTripEventTypeEnum.DRIVER_MATCHED,
                command.getEstimatedPickupTime(),
                Map.of("driverId", command.getDriverId()),
                "DomainTripService"
            )
        );
        DomainDriverMatchedEvent domainEvent = new DomainDriverMatchedEvent(
            trip.getId().getValue(),
            command.getDriverId(),
            command.getEstimatedPickupTime()
        );
        eventStore.saveEvent(domainEvent);
        return trip;
    }

    @Override
    public DomainTripEntity cancelTrip(DomainCancelTripCommand command) {
        DomainTripEntity trip = tripRepository.findById(command.getTripId())
            .orElseThrow(() -> new DomainEntityNotFoundException("Trip", command.getTripId()));
        if (!trip.canBeCancelled()) {
            throw new DomainBusinessRuleViolationException(
                "Trip cannot be cancelled",
                Map.of("currentStatus", trip.getStatus())
            );
        }
        DomainCancellationInfoValue info = new DomainCancellationInfoValue(
            command.getReason(),
            command.getCancelledBy(),
            LocalDateTime.now(),
            command.getRefundAmount()
        );
        trip.cancel(info);
        tripRepository.update(trip);
        timelineRepository.addEvent(trip.getId().getValue(),
            new DomainTripEventValue(
                DomainTripEventTypeEnum.CANCELLED,
                info.getCancelledAt(),
                Map.of("reason", command.getReason().name()),
                "DomainTripService"
            )
        );
        DomainTripCancelledEvent domainEvent = new DomainTripCancelledEvent(
            trip.getId().getValue(),
            command.getReason(),
            info.getCancelledAt()
        );
        eventStore.saveEvent(domainEvent);
        return trip;
    }

    @Override
    public DomainTripEntity completeTrip(String tripId, DomainMoneyValue finalFare) {
        throw new UnsupportedOperationException("completeTrip is not supported in this Shred");
    }

    @Override
    public DomainTripEntity handleMatchingTimeout(DomainMatchingTimeoutCommand command) {
        DomainTripEntity trip = tripRepository.findById(command.getTripId())
            .orElseThrow(() -> new DomainEntityNotFoundException("Trip", command.getTripId()));
        if (!trip.canBeCancelled()) {
            throw new DomainBusinessRuleViolationException(
                "Trip cannot be timed out",
                Map.of("currentStatus", trip.getStatus())
            );
        }
        DomainCancellationInfoValue info = new DomainCancellationInfoValue(
            DomainCancellationReasonEnum.MATCHING_TIMEOUT,
            "system",
            LocalDateTime.now(),
            null
        );
        trip.cancel(info);
        tripRepository.update(trip);
        timelineRepository.addEvent(trip.getId().getValue(),
            new DomainTripEventValue(
                DomainTripEventTypeEnum.MATCH_TIMEOUT,
                LocalDateTime.now(),
                Map.of("reason", command.getReason()),
                "DomainTripService"
            )
        );
        DomainMatchTimeoutEvent domainEvent = new DomainMatchTimeoutEvent(
            trip.getId().getValue(),
            command.getReason(),
            LocalDateTime.now()
        );
        eventStore.saveEvent(domainEvent);
        return trip;
    }

    @Override
    public DomainTripEntity handleInternalTimeout(DomainTimeoutCommand command) {
        return handleMatchingTimeout(
            new DomainMatchingTimeoutCommand(
                command.getTripId(),
                "internal_timeout",
                command.getDuration()
            )
        );
    }
}