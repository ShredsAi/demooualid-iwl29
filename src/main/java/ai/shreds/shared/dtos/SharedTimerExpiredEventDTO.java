package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationTimeoutCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal timer expiration event DTO representing the internal watchdog timeout.
 * This is the failsafe mechanism that triggers if the Matching Service doesn't respond
 * within the configured duration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTimerExpiredEventDTO {

    /** Event type identifier for routing. Always "MATCHING_TIMER_EXPIRED". */
    private String eventType;

    /** The trip ID that has exceeded the timeout duration. */
    private String tripId;

    /** ISO-8601 timestamp when the timeout occurred. */
    private String timeoutAt;

    /** Duration in milliseconds that was exceeded. */
    private Long duration;

    /**
     * Converts this timer event DTO to an application command for processing timeout.
     *
     * @return ApplicationTimeoutCommand for the application layer to process
     */
    public ApplicationTimeoutCommand toApplicationCommand() {
        return ApplicationTimeoutCommand.builder()
                .tripId(tripId)
                .timeoutAt(timeoutAt)
                .duration(duration)
                .build();
    }
}