package ai.shreds.domain.exceptions;

import ai.shreds.domain.enums.DomainTripStatusEnum;

public class DomainInvalidStateTransitionException extends RuntimeException {
    private final DomainTripStatusEnum currentState;
    private final DomainTripStatusEnum attemptedState;

    public DomainInvalidStateTransitionException(DomainTripStatusEnum currentState, DomainTripStatusEnum attemptedState) {
        super(String.format("Invalid state transition from %s to %s", currentState, attemptedState));
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public DomainTripStatusEnum getCurrentState() {
        return currentState;
    }

    public DomainTripStatusEnum getAttemptedState() {
        return attemptedState;
    }
}
