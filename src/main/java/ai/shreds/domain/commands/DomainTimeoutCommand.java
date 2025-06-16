package ai.shreds.domain.commands;

import java.time.LocalDateTime;

public class DomainTimeoutCommand {
    private final String tripId;
    private final LocalDateTime timeoutAt;
    private final Long duration;

    public DomainTimeoutCommand(String tripId, LocalDateTime timeoutAt, Long duration) {
        this.tripId = tripId;
        this.timeoutAt = timeoutAt;
        this.duration = duration;
    }

    public String getTripId() {
        return tripId;
    }

    public LocalDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public Long getDuration() {
        return duration;
    }
}
