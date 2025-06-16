package ai.shreds.domain.services;

import ai.shreds.domain.enums.DomainTripStatusEnum;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class DomainStateMachineService {

    public boolean validateTransition(DomainTripStatusEnum currentState, DomainTripStatusEnum newState) {
        if (currentState == null || newState == null) {
            return false;
        }
        if (currentState == newState) {
            return true;
        }
        switch (currentState) {
            case REQUESTED:
                return newState == DomainTripStatusEnum.MATCHING || newState == DomainTripStatusEnum.CANCELLED;
            case MATCHING:
                return newState == DomainTripStatusEnum.MATCHED
                    || newState == DomainTripStatusEnum.CANCELLED
                    || newState == DomainTripStatusEnum.MATCH_FAILED;
            case MATCHED:
                return newState == DomainTripStatusEnum.DRIVER_EN_ROUTE
                    || newState == DomainTripStatusEnum.CANCELLED;
            case DRIVER_EN_ROUTE:
                return newState == DomainTripStatusEnum.ARRIVED
                    || newState == DomainTripStatusEnum.CANCELLED;
            case ARRIVED:
                return newState == DomainTripStatusEnum.RIDER_PICKED_UP
                    || newState == DomainTripStatusEnum.CANCELLED;
            case RIDER_PICKED_UP:
                return newState == DomainTripStatusEnum.IN_PROGRESS;
            case IN_PROGRESS:
                return newState == DomainTripStatusEnum.COMPLETED
                    || newState == DomainTripStatusEnum.CANCELLED;
            default:
                return false;
        }
    }

    public List<DomainTripStatusEnum> getNextValidStates(DomainTripStatusEnum currentState) {
        if (currentState == null) {
            return Collections.emptyList();
        }
        switch (currentState) {
            case REQUESTED:
                return Arrays.asList(DomainTripStatusEnum.MATCHING, DomainTripStatusEnum.CANCELLED);
            case MATCHING:
                return Arrays.asList(
                    DomainTripStatusEnum.MATCHED,
                    DomainTripStatusEnum.CANCELLED,
                    DomainTripStatusEnum.MATCH_FAILED
                );
            case MATCHED:
                return Arrays.asList(DomainTripStatusEnum.DRIVER_EN_ROUTE, DomainTripStatusEnum.CANCELLED);
            case DRIVER_EN_ROUTE:
                return Arrays.asList(DomainTripStatusEnum.ARRIVED, DomainTripStatusEnum.CANCELLED);
            case ARRIVED:
                return Arrays.asList(DomainTripStatusEnum.RIDER_PICKED_UP, DomainTripStatusEnum.CANCELLED);
            case RIDER_PICKED_UP:
                return Arrays.asList(DomainTripStatusEnum.IN_PROGRESS);
            case IN_PROGRESS:
                return Arrays.asList(DomainTripStatusEnum.COMPLETED, DomainTripStatusEnum.CANCELLED);
            default:
                return Collections.emptyList();
        }
    }

    public boolean isTerminalState(DomainTripStatusEnum status) {
        return status == DomainTripStatusEnum.COMPLETED
            || status == DomainTripStatusEnum.CANCELLED
            || status == DomainTripStatusEnum.MATCH_FAILED;
    }

    public boolean canBeCancelled(DomainTripStatusEnum status) {
        return !isTerminalState(status);
    }
}