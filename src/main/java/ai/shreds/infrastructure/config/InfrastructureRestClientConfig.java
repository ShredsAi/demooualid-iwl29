package ai.shreds.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class InfrastructureRestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    @Bean
    public CircuitBreaker riderServiceCircuitBreaker() {
        return CircuitBreaker.of("riderService", createCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker pricingServiceCircuitBreaker() {
        return CircuitBreaker.of("pricingService", createCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker() {
        return CircuitBreaker.of("paymentService", createCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker driverServiceCircuitBreaker() {
        return CircuitBreaker.of("driverService", createCircuitBreakerConfig());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }

    private CircuitBreakerConfig createCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
    }
}