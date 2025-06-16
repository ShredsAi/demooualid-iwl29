package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationMatchingServiceOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureMessagingException;
import ai.shreds.shared.dtos.SharedMatchingRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@Service
@Slf4j
public class InfrastructureKafkaMatchingProducer implements ApplicationMatchingServiceOutputPort {

    private final KafkaTemplate<String, SharedMatchingRequestDTO> kafkaTemplate;
    private final String matchingRequestTopic;

    public InfrastructureKafkaMatchingProducer(
            KafkaTemplate<String, SharedMatchingRequestDTO> kafkaTemplate,
            @Value("${kafka.topics.matching-requests}") String matchingRequestTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.matchingRequestTopic = matchingRequestTopic;
    }

    @Override
    public void requestMatching(SharedMatchingRequestDTO request) {
        log.debug("Sending matching request for trip ID: {}", request.getTripId());
        
        try {
            // Add correlation ID if not present
            if (request.getCorrelationId() == null || request.getCorrelationId().trim().isEmpty()) {
                request.setCorrelationId("match_req_" + UUID.randomUUID().toString());
            }
            
            ProducerRecord<String, SharedMatchingRequestDTO> record = addHeaders(request);
            
            kafkaTemplate.send(record).addCallback(new ListenableFutureCallback<SendResult<String, SharedMatchingRequestDTO>>() {
                @Override
                public void onSuccess(SendResult<String, SharedMatchingRequestDTO> result) {
                    log.debug("Successfully sent matching request for trip {}: partition={}, offset={}", 
                            request.getTripId(), 
                            result.getRecordMetadata().partition(), 
                            result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send matching request for trip {}: {}", request.getTripId(), ex.getMessage(), ex);
                    // Since this is async, we can't throw here, but we log the error
                }
            });
            
        } catch (Exception e) {
            log.error("Error sending matching request for trip {}: {}", request.getTripId(), e.getMessage(), e);
            throw new InfrastructureMessagingException(matchingRequestTopic, request.getTripId(), e);
        }
    }

    private ProducerRecord<String, SharedMatchingRequestDTO> addHeaders(SharedMatchingRequestDTO request) {
        ProducerRecord<String, SharedMatchingRequestDTO> record = 
                new ProducerRecord<>(matchingRequestTopic, request.getTripId(), request);
        
        // Add headers for correlation and content type
        record.headers().add("correlationId", request.getCorrelationId().getBytes());
        record.headers().add("contentType", "application/json".getBytes());
        record.headers().add("eventType", "MATCHING_REQUEST".getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        record.headers().add("source", "trip-request-matching-shred".getBytes());
        
        return record;
    }
}