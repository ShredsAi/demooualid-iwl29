package ai.shreds.adapter.primary;

import org.springframework.stereotype.Component;

import ai.shreds.shared.dtos.SharedTimerExpiredEventDTO;
import ai.shreds.application.dtos.ApplicationTimeoutCommand;
import ai.shreds.application.ports.ApplicationTimeoutInputPort;

@Component
public class AdapterTimerEventHandler {

    private final ApplicationTimeoutInputPort applicationTimeoutPort;

    public AdapterTimerEventHandler(ApplicationTimeoutInputPort applicationTimeoutPort) {
        this.applicationTimeoutPort = applicationTimeoutPort;
    }

    public void handleMatchingTimerExpired(SharedTimerExpiredEventDTO event) {
        ApplicationTimeoutCommand command = mapToTimeoutCommand(event);
        applicationTimeoutPort.handleTimeout(command);
    }

    private ApplicationTimeoutCommand mapToTimeoutCommand(SharedTimerExpiredEventDTO event) {
        return event.toApplicationCommand();
    }
}
