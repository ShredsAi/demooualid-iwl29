package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationTripExecutionNotificationOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureMessagingException;
import ai.shreds.shared.dtos.SharedTripMatchedMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureTripExecutionNotifier implements ApplicationTripExecutionNotificationOutputPort {

    private final MessageChannel tripExecutionChannel;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void notifyTripMatched(SharedTripMatchedMessageDTO message) {
        log.debug("Notifying Trip Execution Shred: tripId={}, driverId={}", 
                message.getTripId(), message.getDriverId());
        
        try {
            Map<String, Object> headers = createMessageHeaders();
            headers.put("tripId", message.getTripId());
            headers.put("driverId", message.getDriverId());
            headers.put("eventType", "TRIP_MATCHED");
            headers.put("timestamp", System.currentTimeMillis());
            
            var msg = MessageBuilder.createMessage(message, new MessageHeaders(headers));
            
            boolean sent = tripExecutionChannel.send(msg);
            if (!sent) {
                log.error("Failed to send trip matched message for tripId: {}", message.getTripId());
                throw new InfrastructureMessagingException("tripExecutionChannel", message.getTripId(), 
                    new RuntimeException("Message channel rejected the message"));
            }
            
            log.info("Successfully sent trip matched notification for tripId: {}", message.getTripId());
        } catch (Exception e) {
            log.error("Error sending trip matched message for tripId: {}", message.getTripId(), e);
            throw new InfrastructureMessagingException("tripExecutionChannel", message.getTripId(), e);
        }
    }

    private Map<String, Object> createMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("contentType", "application/json");
        headers.put("source", applicationName);
        headers.put("messageType", "TRIP_MATCHED");
        return headers;
    }
}