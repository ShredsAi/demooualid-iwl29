package ai.shreds.adapter.primary;

import org.springframework.stereotype.Component;

import ai.shreds.shared.dtos.SharedDriverAssignedEventDTO;
import ai.shreds.shared.dtos.SharedMatchingTimeoutEventDTO;
import ai.shreds.application.dtos.ApplicationDriverAssignedCommand;
import ai.shreds.application.dtos.ApplicationMatchingTimeoutCommand;
import ai.shreds.application.ports.ApplicationMatchingCoordinatorInputPort;

@Component
public class AdapterMatchingResponseConsumer {

    private final ApplicationMatchingCoordinatorInputPort applicationMatchingCoordinatorPort;

    public AdapterMatchingResponseConsumer(ApplicationMatchingCoordinatorInputPort applicationMatchingCoordinatorPort) {
        this.applicationMatchingCoordinatorPort = applicationMatchingCoordinatorPort;
    }

    public void handleDriverAssigned(SharedDriverAssignedEventDTO event) {
        ApplicationDriverAssignedCommand command = mapToDriverAssignedCommand(event);
        applicationMatchingCoordinatorPort.handleDriverAssigned(command);
    }

    public void handleMatchingTimeout(SharedMatchingTimeoutEventDTO event) {
        ApplicationMatchingTimeoutCommand command = mapToMatchingTimeoutCommand(event);
        applicationMatchingCoordinatorPort.handleMatchingTimeout(command);
    }

    private ApplicationDriverAssignedCommand mapToDriverAssignedCommand(SharedDriverAssignedEventDTO event) {
        return event.toApplicationCommand();
    }

    private ApplicationMatchingTimeoutCommand mapToMatchingTimeoutCommand(SharedMatchingTimeoutEventDTO event) {
        return event.toApplicationCommand();
    }
}
