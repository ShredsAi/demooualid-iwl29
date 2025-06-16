package ai.shreds.application.dtos;

import ai.shreds.domain.commands.DomainTimeoutCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTimeoutCommand {
    private String tripId;
    private String timeoutAt;
    private Long duration;

    public DomainTimeoutCommand toDomainCommand() {
        return new DomainTimeoutCommand(
                tripId,
                timeoutAt != null ? LocalDateTime.parse(timeoutAt) : LocalDateTime.now(),
                duration != null ? duration : 300000L // 5 minutes default
        );
    }
}