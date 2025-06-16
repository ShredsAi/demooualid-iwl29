package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationPaymentServiceOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceUnavailableException;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.dtos.SharedPaymentAuthorizationDTO;
import ai.shreds.shared.dtos.SharedPaymentPreAuthRequestDTO;
import ai.shreds.shared.dtos.SharedPaymentRefundRequestDTO;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class InfrastructurePaymentServiceClient implements ApplicationPaymentServiceOutputPort {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final String paymentServiceUrl;

    public InfrastructurePaymentServiceClient(
            RestTemplate restTemplate,
            @Qualifier("paymentServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Value("${external-services.payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    public SharedPaymentAuthorizationDTO preAuthorize(String riderId, SharedMoneyDTO amount, String tripCorrelationId) {
        log.debug("Pre-authorizing payment for rider {} amount {} {}", riderId, amount.getAmount(), amount.getCurrency());
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    SharedPaymentPreAuthRequestDTO request = createPreAuthRequest(riderId, amount, tripCorrelationId);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<SharedPaymentPreAuthRequestDTO> entity = new HttpEntity<>(request, headers);
                    String url = paymentServiceUrl + "/payments/v1/pre-authorize";
                    SharedPaymentAuthorizationDTO response = restTemplate.postForObject(url, entity, SharedPaymentAuthorizationDTO.class);
                    if (response == null) {
                        log.warn("Payment service returned null for pre-authorization");
                        throw new InfrastructureServiceUnavailableException("Payment Service", HttpStatus.NO_CONTENT.value());
                    }
                    log.debug("Payment pre-authorization successful: authId={}", response.getAuthorizationId());
                    return response;
                } catch (HttpStatusCodeException e) {
                    log.error("HTTP error in payment pre-authorization: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                    if (e.getStatusCode().value() == 402) {
                        return createFailedAuthorizationResponse(riderId, amount, "PAYMENT_DECLINED");
                    }
                    throw new InfrastructureServiceUnavailableException("Payment Service", e.getStatusCode().value());
                } catch (Exception e) {
                    log.error("Error in payment pre-authorization: {}", e.getMessage(), e);
                    throw new InfrastructureServiceUnavailableException("Payment Service", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            });
        } catch (Throwable t) {
            log.warn("Circuit breaker fallback triggered for payment pre-auth: {}", t.getMessage());
            return handlePreAuthFallback(riderId, amount, t);
        }
    }

    @Override
    public Boolean refund(String authorizationId) {
        log.debug("Processing refund for authorization: {}", authorizationId);
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    SharedPaymentRefundRequestDTO request = createRefundRequest(authorizationId);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<SharedPaymentRefundRequestDTO> entity = new HttpEntity<>(request, headers);
                    String url = paymentServiceUrl + "/payments/v1/refund";
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> response = restTemplate.postForObject(url, entity, java.util.Map.class);
                    if (response == null) {
                        log.warn("Payment service returned null for refund");
                        return false;
                    }
                    Boolean refunded = (Boolean) response.get("refunded");
                    log.debug("Refund processed: authId={}, success={}", authorizationId, refunded);
                    return refunded != null ? refunded : false;
                } catch (HttpStatusCodeException e) {
                    log.error("HTTP error in refund processing: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                    if (e.getStatusCode().value() == 404) {
                        log.warn("Authorization not found for refund: {}", authorizationId);
                        return false;
                    }
                    throw new InfrastructureServiceUnavailableException("Payment Service", e.getStatusCode().value());
                } catch (Exception e) {
                    log.error("Error in refund processing: {}", e.getMessage(), e);
                    throw new InfrastructureServiceUnavailableException("Payment Service", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            });
        } catch (Throwable t) {
            log.warn("Circuit breaker fallback triggered for refund: {}", t.getMessage());
            return handleRefundFallback(authorizationId, t);
        }
    }

    private SharedPaymentPreAuthRequestDTO createPreAuthRequest(String riderId, SharedMoneyDTO amount, String tripCorrelationId) {
        SharedPaymentPreAuthRequestDTO request = new SharedPaymentPreAuthRequestDTO();
        request.setRiderId(riderId);
        request.setAmount(amount.getAmount());
        request.setCurrency(amount.getCurrency());
        request.setTripCorrelationId(tripCorrelationId);
        return request;
    }

    private SharedPaymentRefundRequestDTO createRefundRequest(String authorizationId) {
        SharedPaymentRefundRequestDTO request = new SharedPaymentRefundRequestDTO();
        request.setAuthorizationId(authorizationId);
        return request;
    }

    private SharedPaymentAuthorizationDTO handlePreAuthFallback(String riderId, SharedMoneyDTO amount, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for payment pre-auth: {}", ex.getMessage());
        return createFailedAuthorizationResponse(riderId, amount, "SERVICE_UNAVAILABLE");
    }

    private Boolean handleRefundFallback(String authorizationId, Throwable ex) {
        log.warn("Circuit breaker fallback triggered for refund: {}", ex.getMessage());
        return false;
    }

    private SharedPaymentAuthorizationDTO createFailedAuthorizationResponse(String riderId, SharedMoneyDTO amount, String reason) {
        SharedPaymentAuthorizationDTO response = new SharedPaymentAuthorizationDTO();
        response.setAuthorizationId("FAILED_" + System.currentTimeMillis());
        response.setRiderId(riderId);
        response.setAmount(amount);
        response.setExpiry(LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.setStatus("FAILED");
        return response;
    }
}
