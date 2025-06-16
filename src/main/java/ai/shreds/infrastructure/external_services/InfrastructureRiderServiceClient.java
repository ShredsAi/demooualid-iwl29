package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationRiderServiceOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException;
import ai.shreds.shared.dtos.SharedRiderProfileDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@Slf4j
public class InfrastructureRiderServiceClient implements ApplicationRiderServiceOutputPort {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final String riderServiceUrl;

    public InfrastructureRiderServiceClient(
            RestTemplate restTemplate,
            @Qualifier("riderServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Value("${external-services.rider-service.url}") String riderServiceUrl) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.riderServiceUrl = riderServiceUrl;
    }

    @Override
    @Cacheable(value = "riderProfiles", key = "#riderId", unless = "#result == null")
    public SharedRiderProfileDTO validateRider(String riderId) {
        log.debug("Validating rider with ID: {}", riderId);
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                String url = riderServiceUrl + "/riders/" + riderId;
                SharedRiderProfileDTO response = restTemplate.getForObject(url, SharedRiderProfileDTO.class);
                
                if (response == null) {
                    log.warn("Rider service returned null for rider ID: {}", riderId);
                    throw new InfrastructureServiceUnavailableException("Rider Service", HttpStatus.NO_CONTENT.value());
                }
                
                log.debug("Rider validation successful for ID: {}", riderId);
                return response;
            } catch (HttpStatusCodeException e) {
                log.error("HTTP error validating rider {}: {} {}", riderId, e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 404) {
                    // Return a rider with suspended status for not found
                    return createSuspendedRiderResponse(riderId, "RIDER_NOT_FOUND");
                }
                throw new InfrastructureServiceUnavailableException("Rider Service", e.getStatusCode().value());
            } catch (Exception e) {
                log.error("Error validating rider {}: {}", riderId, e.getMessage(), e);
                throw new InfrastructureServiceUnavailableException("Rider Service", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }, throwable -> handleFallback(riderId, throwable));
    }

    private SharedRiderProfileDTO handleFallback(String riderId, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for rider {}: {}", riderId, ex.getMessage());
        
        // Return a default suspended rider profile to prevent trip creation
        return createSuspendedRiderResponse(riderId, "SERVICE_UNAVAILABLE");
    }

    private SharedRiderProfileDTO createSuspendedRiderResponse(String riderId, String reason) {
        SharedRiderProfileDTO fallbackResponse = new SharedRiderProfileDTO();
        fallbackResponse.setRiderId(riderId);
        fallbackResponse.setStatus("SUSPENDED");
        fallbackResponse.setRating(BigDecimal.ZERO);
        fallbackResponse.setFlags(Collections.singletonList("SERVICE_VALIDATION_FAILED"));
        fallbackResponse.setSuspensionReason(reason);
        return fallbackResponse;
    }
}