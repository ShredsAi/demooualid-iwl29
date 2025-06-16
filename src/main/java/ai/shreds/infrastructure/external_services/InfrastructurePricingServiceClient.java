package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationPricingServiceOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException;
import ai.shreds.shared.dtos.SharedFareEstimateDTO;
import ai.shreds.shared.dtos.SharedLocationDTO;
import ai.shreds.shared.dtos.SharedPricingRequestDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class InfrastructurePricingServiceClient implements ApplicationPricingServiceOutputPort {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final String pricingServiceUrl;

    public InfrastructurePricingServiceClient(
            RestTemplate restTemplate,
            @Qualifier("pricingServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Value("${external-services.pricing-service.url}") String pricingServiceUrl) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.pricingServiceUrl = pricingServiceUrl;
    }

    @Override
    public SharedFareEstimateDTO estimateFare(SharedLocationDTO pickupLocation, SharedLocationDTO dropoffLocation, Map<String, String> metadata) {
        log.debug("Estimating fare from {} to {}", pickupLocation.getAddress(), dropoffLocation.getAddress());
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                SharedPricingRequestDTO request = createPricingRequest(pickupLocation, dropoffLocation, metadata);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<SharedPricingRequestDTO> entity = new HttpEntity<>(request, headers);
                
                String url = pricingServiceUrl + "/pricing/v1/estimate";
                SharedFareEstimateDTO response = restTemplate.postForObject(url, entity, SharedFareEstimateDTO.class);
                
                if (response == null) {
                    log.warn("Pricing service returned null for route");
                    throw new InfrastructureServiceUnavailableException("Pricing Service", HttpStatus.NO_CONTENT.value());
                }
                
                log.debug("Fare estimate successful: {} {}", response.getAmount(), response.getCurrency());
                return response;
            } catch (HttpStatusCodeException e) {
                log.error("HTTP error estimating fare: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new InfrastructureServiceUnavailableException("Pricing Service", e.getStatusCode().value());
            } catch (Exception e) {
                log.error("Error estimating fare: {}", e.getMessage(), e);
                throw new InfrastructureServiceUnavailableException("Pricing Service", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }, throwable -> handleFallback(pickupLocation, dropoffLocation, throwable));
    }

    private SharedPricingRequestDTO createPricingRequest(SharedLocationDTO pickup, SharedLocationDTO dropoff, Map<String, String> metadata) {
        SharedPricingRequestDTO request = new SharedPricingRequestDTO();
        request.setPickupLocation(pickup);
        request.setDropoffLocation(dropoff);
        request.setMetadata(metadata != null ? metadata : Map.of());
        return request;
    }

    private SharedFareEstimateDTO handleFallback(SharedLocationDTO pickup, SharedLocationDTO dropoff, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for pricing: {}", ex.getMessage());
        
        // Calculate basic fare estimate based on straight-line distance
        double distance = calculateStraightLineDistance(pickup, dropoff);
        BigDecimal baseFare = BigDecimal.valueOf(5.0); // Base fare
        BigDecimal perKmRate = BigDecimal.valueOf(2.0); // Per km rate
        BigDecimal estimatedFare = baseFare.add(perKmRate.multiply(BigDecimal.valueOf(distance)));
        
        SharedFareEstimateDTO fallbackEstimate = new SharedFareEstimateDTO();
        fallbackEstimate.setAmount(estimatedFare);
        fallbackEstimate.setCurrency("USD");
        fallbackEstimate.setSurgeMultiplier(BigDecimal.ONE);
        fallbackEstimate.setEstimatedDuration(Long.valueOf((long)(distance * 3))); // Rough estimate: 3 minutes per km
        fallbackEstimate.setEstimatedDistance(BigDecimal.valueOf(distance));
        
        return fallbackEstimate;
    }

    private double calculateStraightLineDistance(SharedLocationDTO from, SharedLocationDTO to) {
        double earthRadius = 6371.0; // km
        double lat1 = Math.toRadians(from.getLatitude().doubleValue());
        double lat2 = Math.toRadians(to.getLatitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to.getLongitude().doubleValue() - from.getLongitude().doubleValue());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
}