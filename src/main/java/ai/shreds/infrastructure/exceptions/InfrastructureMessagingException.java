package ai.shreds.infrastructure.exceptions;

import lombok.Getter;

/**
 * Exception thrown when messaging operations fail (Kafka, Spring Messaging, etc.)
 */
@Getter
public class InfrastructureMessagingException extends RuntimeException {

    private final String topic;
    private final String messageKey;

    public InfrastructureMessagingException(String topic, String messageKey, Throwable cause) {
        super(String.format("Messaging operation failed for topic '%s' with key '%s': %s", 
                topic, messageKey, cause.getMessage()), cause);
        this.topic = topic;
        this.messageKey = messageKey;
    }

    public InfrastructureMessagingException(String topic, String messageKey, String message) {
        super(String.format("Messaging operation failed for topic '%s' with key '%s': %s", 
                topic, messageKey, message));
        this.topic = topic;
        this.messageKey = messageKey;
    }

    public InfrastructureMessagingException(String topic, String messageKey, String message, Throwable cause) {
        super(String.format("Messaging operation failed for topic '%s' with key '%s': %s", 
                topic, messageKey, message), cause);
        this.topic = topic;
        this.messageKey = messageKey;
    }
}