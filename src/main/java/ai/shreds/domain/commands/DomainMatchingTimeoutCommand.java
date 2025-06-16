package ai.shreds.domain.commands;

public class DomainMatchingTimeoutCommand {
    private final String tripId;
    private final String reason;
    private final Long timeoutDuration;

    public DomainMatchingTimeoutCommand(String tripId, String reason, Long timeoutDuration) {
        this.tripId = tripId;
        this.reason = reason;
        this.timeoutDuration = timeoutDuration;
    }

    public String getTripId() {
        return tripId;
    }

    public String getReason() {
        return reason;
    }

    public Long getTimeoutDuration() {
        return timeoutDuration;
    }
}
