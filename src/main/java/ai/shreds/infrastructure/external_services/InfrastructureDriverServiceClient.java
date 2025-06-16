package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationDriverServiceOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException;
import ai.shreds.shared.dtos.SharedDriverProfileDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@Slf4j
public class InfrastructureDriverServiceClient implements ApplicationDriverServiceOutputPort {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final String driverServiceUrl;

    public InfrastructureDriverServiceClient(
            RestTemplate restTemplate,
            @Qualifier("driverServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Value("${external-services.driver-service.url}") String driverServiceUrl) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.driverServiceUrl = driverServiceUrl;
    }

    @Override
    @Cacheable(value = "driverProfiles", key = "#driverId", unless = "#result == null")
    public SharedDriverProfileDTO validateDriver(String driverId) {
        log.debug("Validating driver with ID: {}", driverId);
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                String url = driverServiceUrl + "/drivers/" + driverId;
                SharedDriverProfileDTO response = restTemplate.getForObject(url, SharedDriverProfileDTO.class);
                
                if (response == null) {
                    log.warn("Driver service returned null for driver ID: {}", driverId);
                    throw new InfrastructureServiceUnavailableException("Driver Service", HttpStatus.NO_CONTENT.value());
                }
                
                log.debug("Driver validation successful for ID: {}", driverId);
                return response;
            } catch (HttpStatusCodeException e) {
                log.error("HTTP error validating driver {}: {} {}", driverId, e.getStatusCode(), e.getResponseBodyAsString());
                
                if (e.getStatusCode().value() == 404) {
                    // Return an offline driver for not found
                    return createOfflineDriverResponse(driverId, "DRIVER_NOT_FOUND");
                }
                
                throw new InfrastructureServiceUnavailableException("Driver Service", e.getStatusCode().value());
            } catch (Exception e) {
                log.error("Error validating driver {}: {}", driverId, e.getMessage(), e);
                throw new InfrastructureServiceUnavailableException("Driver Service", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }, throwable -> handleFallback(driverId, throwable));
    }

    private SharedDriverProfileDTO handleFallback(String driverId, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for driver {}: {}", driverId, ex.getMessage());
        
        // Return a default offline driver profile to prevent matching
        return createOfflineDriverResponse(driverId, "SERVICE_UNAVAILABLE");
    }

    private SharedDriverProfileDTO createOfflineDriverResponse(String driverId, String reason) {
        SharedDriverProfileDTO fallbackResponse = new SharedDriverProfileDTO();
        fallbackResponse.setDriverId(driverId);
        fallbackResponse.setStatus("INACTIVE");
        fallbackResponse.setOnlineState("OFFLINE");
        fallbackResponse.setVehicleType("UNKNOWN");
        fallbackResponse.setRating(BigDecimal.ZERO);
        return fallbackResponse;
    }
}