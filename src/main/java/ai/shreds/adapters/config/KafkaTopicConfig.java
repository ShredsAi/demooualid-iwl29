package ai.shreds.adapters.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("kafkaTopicConfig")
@Getter
public class KafkaTopicConfig {

    @Value("${kafka.topics.trip-lifecycle-events:trip-lifecycle-events}")
    private String tripLifecycleEventsTopic;

    @Value("${kafka.topics.matching-requests:matching-requests}")
    private String matchingRequestsTopic;

    @Value("${kafka.topics.matching-service-responses:matching-service-responses}")
    private String matchingServiceResponsesTopic;

    @Value("${kafka.consumer.group-id:trip-request-matching-group}")
    private String consumerGroupId;

    @Value("${kafka.producer.acks:all}")
    private String producerAcks;

    @Value("${kafka.producer.enable-idempotence:true}")
    private boolean enableIdempotence;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;

    @Value("${kafka.consumer.max-poll-records:10}")
    private int maxPollRecords;

    @Value("${kafka.consumer.session.timeout.ms:30000}")
    private int sessionTimeoutMs;

    @Value("${kafka.consumer.heartbeat.interval.ms:3000}")
    private int heartbeatIntervalMs;

    @Value("${kafka.producer.retries:3}")
    private int producerRetries;

    @Value("${kafka.producer.retry.backoff.ms:1000}")
    private int retryBackoffMs;

    @Value("${kafka.producer.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    public boolean isValidTopic(String topic) {
        return topic != null && !topic.trim().isEmpty();
    }

    public String getFullTopicName(String baseTopic, String environment) {
        if (environment != null && !environment.isEmpty() && !"default".equals(environment)) {
            return String.format("%s-%s", baseTopic, environment);
        }
        return baseTopic;
    }
}