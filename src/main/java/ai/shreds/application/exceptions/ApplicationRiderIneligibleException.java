package ai.shreds.application.exceptions;

public class ApplicationRiderIneligibleException extends RuntimeException {

    private final String riderId;
    private final String reason;

    public ApplicationRiderIneligibleException(String riderId, String reason) {
        super("Rider " + riderId + " is not eligible: " + reason);
        this.riderId = riderId;
        this.reason = reason;
    }

    public String getRiderId() {
        return riderId;
    }

    public String getReason() {
        return reason;
    }
}