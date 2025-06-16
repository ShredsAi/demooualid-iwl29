package ai.shreds.application.dtos;

import ai.shreds.domain.commands.DomainMatchingTimeoutCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMatchingTimeoutCommand {
    private String tripId;
    private String reason;
    private Long timeoutDuration;
    private String correlationId;

    public DomainMatchingTimeoutCommand toDomainCommand() {
        return new DomainMatchingTimeoutCommand(
                tripId,
                reason != null ? reason : "MATCHING_SERVICE_TIMEOUT",
                timeoutDuration != null ? timeoutDuration : 300000L // 5 minutes default
        );
    }
}