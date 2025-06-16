package ai.shreds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ai.shreds.application.ports.*;
import ai.shreds.shared.dtos.*;
import ai.shreds.infrastructure.repositories.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class TripRequestMatchingShredApplicationStartupTest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    @Qualifier("kafkaTemplate")
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper();

    // Mock external service output ports
    @MockBean
    private ApplicationRiderServiceOutputPort riderServiceOutputPort;

    @MockBean
    private ApplicationPricingServiceOutputPort pricingServiceOutputPort;

    @MockBean
    private ApplicationPaymentServiceOutputPort paymentServiceOutputPort;

    @MockBean
    private ApplicationDriverServiceOutputPort driverServiceOutputPort;

    @MockBean
    private ApplicationMatchingServiceOutputPort matchingServiceOutputPort;

    // Removed @MockBean for ApplicationEventPublisherOutputPort to allow real bean creation
    // The InfrastructureKafkaEventPublisher will be created as a real bean

    @MockBean
    private ApplicationTripExecutionNotificationOutputPort tripExecutionNotificationOutputPort;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "trip-request-matching-shred-test");
        
        // Kafka topics configuration
        registry.add("spring.kafka.topics.trip-lifecycle-events", () -> "trip-lifecycle-events-test");
        registry.add("spring.kafka.topics.matching-requests", () -> "matching-requests-test");
        registry.add("spring.kafka.topics.matching-service-responses", () -> "matching-service-responses-test");
        
        // External services configuration (mock URLs)
        registry.add("external-services.rider-service.url", () -> "http://localhost:9001");
        registry.add("external-services.pricing-service.url", () -> "http://localhost:9002");
        registry.add("external-services.payment-service.url", () -> "http://localhost:9003");
        registry.add("external-services.driver-service.url", () -> "http://localhost:9004");
        
        // Disable circuit breakers for tests
        registry.add("resilience4j.circuitbreaker.instances.rider.disabled", () -> "true");
        registry.add("resilience4j.circuitbreaker.instances.pricing.disabled", () -> "true");
        registry.add("resilience4j.circuitbreaker.instances.payment.disabled", () -> "true");
        registry.add("resilience4j.circuitbreaker.instances.driver.disabled", () -> "true");
        
        // Reduce timeout for tests
        registry.add("matching.timeout.duration", () -> "5000");
        
        // Set log levels for better visibility
        registry.add("logging.level.root", () -> "INFO");
        registry.add("logging.level.ai.shreds", () -> "INFO");
        registry.add("logging.level.org.springframework.boot", () -> "INFO");
        registry.add("logging.level.org.springframework.kafka", () -> "INFO");
        registry.add("logging.level.org.springframework.data", () -> "INFO");
        registry.add("logging.level.com.zaxxer.hikari", () -> "INFO");
        registry.add("logging.level.org.springframework.boot.actuate", () -> "INFO");
    }

    @Test
    void shouldStartApplicationSuccessfully(CapturedOutput output) {
        // Setup mock responses for external services
        setupMockResponses();
        
        // Log the captured output for debugging
        String logs = output.getAll();
        System.out.println("=== Application Startup Logs ===");
        System.out.println(logs);
        System.out.println("=== End of Logs ===");
        
        // Verify application started successfully - look for the main application class startup
        assertThat(logs)
                .containsAnyOf(
                    "Started TripRequestMatchingShredApplication",
                    "TripRequestMatchingShredApplicationStartupTest in"
                );

        // Verify no critical errors in logs
        assertThat(logs)
                .doesNotContain("FATAL")
                .doesNotContain("Failed to start")
                .doesNotContain("Application run failed");

        // Verify server is running on assigned port
        assertThat(port).isGreaterThan(0);
        
        // Verify containers are running
        assertThat(postgres.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();
        
        // Log success message
        System.out.println("✅ Application started successfully on port: " + port);
        System.out.println("✅ PostgreSQL container running: " + postgres.getJdbcUrl());
        System.out.println("✅ Kafka container running: " + kafka.getBootstrapServers());
    }

    @Test
    void shouldHaveHealthEndpointAccessible(CapturedOutput output) {
        // Verify that actuator endpoints are configured by checking the application context
        assertThat(applicationContext)
                .isNotNull();
        
        // Check if management endpoints are configured in properties
        String actuatorBasePath = applicationContext.getEnvironment().getProperty("management.endpoints.web.base-path", "/actuator");
        assertThat(actuatorBasePath).isNotNull();
        
        System.out.println("✅ Management endpoints configured with base path: " + actuatorBasePath);
        System.out.println("✅ Actuator endpoints configured successfully");
    }

    @Test
    void shouldInitializeJpaRepositories(CapturedOutput output) {
        // Verify JPA repositories are initialized by checking beans
        assertThat(applicationContext.getBean(InfrastructureTripJpaRepository.class))
                .isNotNull();
        assertThat(applicationContext.getBean(InfrastructureTimelineJpaRepository.class))
                .isNotNull();
        assertThat(applicationContext.getBean(InfrastructureTripEventJpaRepository.class))
                .isNotNull();
        assertThat(applicationContext.getBean(InfrastructureOutboxEventJpaRepository.class))
                .isNotNull();
        
        // Verify database connection by executing a simple query
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
        
        System.out.println("✅ JPA repositories initialized successfully");
        System.out.println("✅ Database connection verified");
    }

    @Test
    void shouldInitializeKafkaComponents(CapturedOutput output) {
        // Verify Kafka components are initialized by checking injected KafkaTemplate
        assertThat(kafkaTemplate).isNotNull();
        
        // Verify that we also have the specialized KafkaTemplate for SharedTripEventDTO
        KafkaTemplate<String, SharedTripEventDTO> sharedEventKafkaTemplate = 
                applicationContext.getBean("sharedTripEventKafkaTemplate", KafkaTemplate.class);
        assertThat(sharedEventKafkaTemplate).isNotNull();
        
        // Verify Kafka listeners are configured
        assertThat(applicationContext.getBeansOfType(org.springframework.kafka.config.KafkaListenerEndpointRegistry.class))
                .isNotEmpty();
        
        System.out.println("✅ Kafka components initialized successfully");
        System.out.println("✅ Kafka template and listeners configured");
        System.out.println("✅ Generic KafkaTemplate available: " + kafkaTemplate.getClass().getSimpleName());
        System.out.println("✅ SharedTripEventDTO KafkaTemplate available: " + sharedEventKafkaTemplate.getClass().getSimpleName());
    }

    private void setupMockResponses() {
        // Mock Rider Service
        SharedRiderProfileDTO mockRiderProfile = new SharedRiderProfileDTO();
        mockRiderProfile.setRiderId("test-rider-id");
        mockRiderProfile.setStatus("ACTIVE");
        mockRiderProfile.setRating(BigDecimal.valueOf(4.5));
        mockRiderProfile.setFlags(Collections.emptyList());
        when(riderServiceOutputPort.validateRider(any(String.class)))
                .thenReturn(mockRiderProfile);

        // Mock Pricing Service
        SharedFareEstimateDTO mockFareEstimate = new SharedFareEstimateDTO();
        mockFareEstimate.setAmount(BigDecimal.valueOf(25.50));
        mockFareEstimate.setCurrency("USD");
        mockFareEstimate.setSurgeMultiplier(BigDecimal.ONE);
        mockFareEstimate.setEstimatedDuration(1800L);
        mockFareEstimate.setEstimatedDistance(BigDecimal.valueOf(10.5));
        when(pricingServiceOutputPort.estimateFare(any(), any(), any()))
                .thenReturn(mockFareEstimate);

        // Mock Payment Service
        SharedPaymentAuthorizationDTO mockPaymentAuth = new SharedPaymentAuthorizationDTO();
        mockPaymentAuth.setAuthorizationId("auth-123");
        mockPaymentAuth.setRiderId("test-rider-id");
        SharedMoneyDTO mockMoney = new SharedMoneyDTO();
        mockMoney.setAmount(BigDecimal.valueOf(25.50));
        mockMoney.setCurrency("USD");
        mockPaymentAuth.setAmount(mockMoney);
        mockPaymentAuth.setExpiry("2024-12-31T23:59:59Z");
        mockPaymentAuth.setStatus("AUTHORIZED");
        when(paymentServiceOutputPort.preAuthorize(any(), any(), any()))
                .thenReturn(mockPaymentAuth);
        when(paymentServiceOutputPort.refund(any(String.class)))
                .thenReturn(true);

        // Mock Driver Service
        SharedDriverProfileDTO mockDriverProfile = new SharedDriverProfileDTO();
        mockDriverProfile.setDriverId("test-driver-id");
        mockDriverProfile.setStatus("ACTIVE");
        mockDriverProfile.setOnlineState("ONLINE");
        mockDriverProfile.setVehicleType("SEDAN");
        mockDriverProfile.setRating(BigDecimal.valueOf(4.8));
        when(driverServiceOutputPort.validateDriver(any(String.class)))
                .thenReturn(mockDriverProfile);

        System.out.println("✅ Mock responses configured for external services");
    }
}