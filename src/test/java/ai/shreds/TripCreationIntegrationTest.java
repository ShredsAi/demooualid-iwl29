package ai.shreds;

import ai.shreds.application.ports.*;
import ai.shreds.infrastructure.external_services.InfrastructureKafkaEventPublisher;
import ai.shreds.infrastructure.repositories.*;
import ai.shreds.shared.dtos.*;
import ai.shreds.shared.enums.SharedEnumTripStatus;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class TripCreationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InfrastructureTripJpaRepository tripJpaRepository;

    @Autowired
    private InfrastructureOutboxEventJpaRepository outboxEventJpaRepository;

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

    @MockBean
    private ApplicationTripExecutionNotificationOutputPort tripExecutionNotificationOutputPort;

    // Keep the real event publisher to test Kafka integration
    @Autowired
    private InfrastructureKafkaEventPublisher kafkaEventPublisher;

    private Consumer<String, SharedTripEventDTO> tripEventConsumer;
    private Consumer<String, SharedMatchingRequestDTO> matchingRequestConsumer;

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
        registry.add("logging.level.ai.shreds", () -> "DEBUG");
    }

    @BeforeEach
    void setUp() {
        // Clear database before each test
        jdbcTemplate.execute("DELETE FROM trip_event");
        jdbcTemplate.execute("DELETE FROM trip_timeline");
        jdbcTemplate.execute("DELETE FROM outbox_event");
        jdbcTemplate.execute("DELETE FROM trip");
        
        // Reset mocks
        reset(riderServiceOutputPort, pricingServiceOutputPort, paymentServiceOutputPort, 
              driverServiceOutputPort, matchingServiceOutputPort, tripExecutionNotificationOutputPort);
        
        // Setup Kafka consumers for testing
        setupKafkaConsumers();
    }

    @Test
    void When_Valid_Trip_Request_Then_Trip_Is_Created_And_Matching_Started(CapturedOutput output) {
        // Given: A valid trip request
        SharedTripRequestDTO tripRequest = createValidTripRequest();
        
        // Setup mock responses for successful scenario
        setupSuccessfulMockResponses();
        
        System.out.println("🔄 Starting trip creation test...");
        System.out.println("📍 Trip request: " + tripRequest.getRiderId());
        
        // When: Trip is created via REST API
        ResponseEntity<SharedTripResponseDTO> response = restTemplate.postForEntity(
            "/api/v1/trips", 
            tripRequest, 
            SharedTripResponseDTO.class
        );
        
        // Then: HTTP 201 Created response is returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        SharedTripResponseDTO tripResponse = response.getBody();
        System.out.println("✅ Trip created with ID: " + tripResponse.getTripId());
        
        // And: Trip has correct initial status and data
        assertThat(tripResponse.getTripId()).isNotBlank();
        assertThat(tripResponse.getStatus()).isEqualTo(SharedEnumTripStatus.REQUESTED.name());
        assertThat(tripResponse.getEstimatedFare()).isNotNull();
        // Fix BigDecimal comparison to handle scale properly
        assertThat(tripResponse.getEstimatedFare().getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(tripResponse.getEstimatedFare().getCurrency()).isEqualTo("USD");
        assertThat(tripResponse.getRequestedAt()).isNotNull();
        
        // And: Trip is persisted in database with REQUESTED status
        UUID tripUuid = UUID.fromString(tripResponse.getTripId());
        var tripInDb = tripJpaRepository.findById(tripUuid);
        assertThat(tripInDb).isPresent();
        // Fix enum comparison: compare domain enum to domain enum, not to shared enum string
        assertThat(tripInDb.get().getStatus()).isEqualTo(DomainTripStatusEnum.REQUESTED);
        assertThat(tripInDb.get().getRiderId()).isEqualTo(UUID.fromString(tripRequest.getRiderId()));
        
        System.out.println("✅ Trip persisted in database with status: " + tripInDb.get().getStatus());
        
        // And: Payment pre-authorization was called
        ArgumentCaptor<String> riderIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SharedMoneyDTO> amountCaptor = ArgumentCaptor.forClass(SharedMoneyDTO.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(paymentServiceOutputPort, times(1)).preAuthorize(
            riderIdCaptor.capture(), 
            amountCaptor.capture(), 
            correlationIdCaptor.capture()
        );
        
        assertThat(riderIdCaptor.getValue()).isEqualTo(tripRequest.getRiderId());
        // Fix BigDecimal comparison in payment verification as well
        assertThat(amountCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(correlationIdCaptor.getValue()).isEqualTo(tripResponse.getTripId());
        
        System.out.println("✅ Payment pre-authorization called for amount: " + amountCaptor.getValue().getAmount());
        
        // And: TRIP_REQUESTED event is published to Kafka
        List<SharedTripEventDTO> tripEvents = consumeKafkaMessages(tripEventConsumer, "trip-lifecycle-events-test", 1, Duration.ofSeconds(10));
        assertThat(tripEvents).hasSize(1);
        
        SharedTripEventDTO tripEvent = tripEvents.get(0);
        assertThat(tripEvent.getEventType()).isEqualTo("TRIP_REQUESTED");
        assertThat(tripEvent.getTripId()).isEqualTo(tripResponse.getTripId());
        assertThat(tripEvent.getSource()).isEqualTo("trip-request-matching-shred");
        
        System.out.println("✅ TRIP_REQUESTED event published to Kafka: " + tripEvent.getEventType());
        
        // And: Matching request is sent to Matching Service
        ArgumentCaptor<SharedMatchingRequestDTO> matchingRequestCaptor = ArgumentCaptor.forClass(SharedMatchingRequestDTO.class);
        
        verify(matchingServiceOutputPort, times(1)).requestMatching(matchingRequestCaptor.capture());
        
        SharedMatchingRequestDTO matchingRequest = matchingRequestCaptor.getValue();
        assertThat(matchingRequest.getTripId()).isEqualTo(tripResponse.getTripId());
        assertThat(matchingRequest.getRiderId()).isEqualTo(tripRequest.getRiderId());
        assertThat(matchingRequest.getPickupLocation()).isNotNull();
        assertThat(matchingRequest.getDropoffLocation()).isNotNull();
        
        System.out.println("✅ Matching request sent to Matching Service for trip: " + matchingRequest.getTripId());
        
        // And: All external services were called correctly
        verify(riderServiceOutputPort, times(1)).validateRider(tripRequest.getRiderId());
        verify(pricingServiceOutputPort, times(1)).estimateFare(any(), any(), any());
        
        // And: No errors in logs
        String logs = output.getAll();
        assertThat(logs).doesNotContain("ERROR").doesNotContain("FATAL");
        
        System.out.println("🎉 Trip creation workflow completed successfully!");
    }

    @Test
    void When_Payment_PreAuthorization_Fails_Then_Trip_Creation_Is_Rejected(CapturedOutput output) {
        // Given: A valid trip request
        SharedTripRequestDTO tripRequest = createValidTripRequest();
        
        // Setup mock responses where payment fails
        setupPaymentFailureMockResponses();
        
        System.out.println("🔄 Starting payment failure test...");
        System.out.println("📍 Trip request: " + tripRequest.getRiderId());
        
        // When: Trip creation is attempted via REST API
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/trips", 
            tripRequest, 
            Map.class // Using Map to capture error response
        );
        
        // Then: HTTP 402 Payment Required is returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody()).isNotNull();
        
        System.out.println("✅ HTTP 402 Payment Required returned as expected");
        
        // And: Error response contains payment failure details
        Map<String, Object> errorResponse = response.getBody();
        assertThat(errorResponse.get("code")).isEqualTo("PAYMENT_FAILED");
        assertThat(errorResponse.get("message")).isEqualTo("Payment authorization failed");
        
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) errorResponse.get("details");
        assertThat(details.get("riderId")).isEqualTo(tripRequest.getRiderId());
        assertThat(details.get("errorCode")).isEqualTo("Payment authorization failed");
        
        System.out.println("✅ Error response contains expected payment failure details");
        
        // And: No trip is created in the database
        List<UUID> tripsInDb = jdbcTemplate.queryForList(
            "SELECT trip_id FROM trip WHERE rider_id = ?::uuid", 
            UUID.class, 
            tripRequest.getRiderId()
        );
        assertThat(tripsInDb).isEmpty();
        
        System.out.println("✅ No trip was persisted in database");
        
        // And: No events are published to Kafka
        List<SharedTripEventDTO> tripEvents = consumeKafkaMessages(tripEventConsumer, "trip-lifecycle-events-test", 0, Duration.ofSeconds(3));
        assertThat(tripEvents).isEmpty();
        
        System.out.println("✅ No events were published to Kafka");
        
        // And: No outbox events are created
        List<Map<String, Object>> outboxEvents = jdbcTemplate.queryForList(
            "SELECT * FROM outbox_event"
        );
        assertThat(outboxEvents).isEmpty();
        
        System.out.println("✅ No outbox events were created");
        
        // And: Payment pre-authorization was attempted
        verify(paymentServiceOutputPort, times(1)).preAuthorize(any(), any(), any());
        
        // And: No matching request is sent
        verify(matchingServiceOutputPort, never()).requestMatching(any());
        
        System.out.println("✅ No matching request was sent");
        
        // And: Rider validation and pricing were still called (before payment failure)
        verify(riderServiceOutputPort, times(1)).validateRider(tripRequest.getRiderId());
        verify(pricingServiceOutputPort, times(1)).estimateFare(any(), any(), any());
        
        System.out.println("✅ Rider validation and pricing estimation were called as expected");
        
        // And: Application logs contain the payment failure
        String logs = output.getAll();
        assertThat(logs).contains("Payment failed for rider");
        
        System.out.println("🎉 Payment failure scenario completed successfully!");
    }

    private SharedTripRequestDTO createValidTripRequest() {
        SharedLocationDTO pickupLocation = SharedLocationDTO.builder()
            .latitude(BigDecimal.valueOf(37.7749))
            .longitude(BigDecimal.valueOf(-122.4194))
            .address("123 Market Street")
            .city("San Francisco")
            .postalCode("94102")
            .build();
            
        SharedLocationDTO dropoffLocation = SharedLocationDTO.builder()
            .latitude(BigDecimal.valueOf(37.7849))
            .longitude(BigDecimal.valueOf(-122.4094))
            .address("456 Mission Street")
            .city("San Francisco")
            .postalCode("94103")
            .build();
            
        return SharedTripRequestDTO.builder()
            .riderId("550e8400-e29b-41d4-a716-446655440000")
            .pickupLocation(pickupLocation)
            .dropoffLocation(dropoffLocation)
            .scheduledFor(null) // Immediate trip
            .metadata(Collections.emptyMap())
            .build();
    }

    private void setupSuccessfulMockResponses() {
        // Mock Rider Service
        SharedRiderProfileDTO mockRiderProfile = new SharedRiderProfileDTO();
        mockRiderProfile.setRiderId("550e8400-e29b-41d4-a716-446655440000");
        mockRiderProfile.setStatus("ACTIVE");
        mockRiderProfile.setRating(BigDecimal.valueOf(4.5));
        mockRiderProfile.setFlags(Collections.emptyList());
        when(riderServiceOutputPort.validateRider(anyString()))
                .thenReturn(mockRiderProfile);

        // Mock Pricing Service
        SharedFareEstimateDTO mockFareEstimate = new SharedFareEstimateDTO();
        mockFareEstimate.setAmount(new BigDecimal("25.50")); // Use string constructor to preserve scale
        mockFareEstimate.setCurrency("USD");
        mockFareEstimate.setSurgeMultiplier(BigDecimal.ONE);
        mockFareEstimate.setEstimatedDuration(1800L);
        mockFareEstimate.setEstimatedDistance(BigDecimal.valueOf(10.5));
        when(pricingServiceOutputPort.estimateFare(any(), any(), any()))
                .thenReturn(mockFareEstimate);

        // Mock Payment Service - Return successful pre-authorization with FUTURE expiry date
        SharedPaymentAuthorizationDTO mockPaymentAuth = new SharedPaymentAuthorizationDTO();
        mockPaymentAuth.setAuthorizationId("auth-123");
        mockPaymentAuth.setRiderId("550e8400-e29b-41d4-a716-446655440000");
        SharedMoneyDTO mockMoney = new SharedMoneyDTO();
        mockMoney.setAmount(new BigDecimal("25.50")); // Use string constructor to preserve scale
        mockMoney.setCurrency("USD");
        mockPaymentAuth.setAmount(mockMoney);
        // Use a future date to ensure authorization doesn't expire during test
        String futureExpiryDate = OffsetDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        mockPaymentAuth.setExpiry(futureExpiryDate);
        mockPaymentAuth.setStatus("AUTHORIZED");
        when(paymentServiceOutputPort.preAuthorize(any(), any(), any()))
                .thenReturn(mockPaymentAuth);
        
        System.out.println("✅ Mock responses configured for successful scenario");
        System.out.println("📅 Payment authorization expiry set to: " + futureExpiryDate);
    }

    private void setupPaymentFailureMockResponses() {
        // Mock Rider Service (same as successful case)
        SharedRiderProfileDTO mockRiderProfile = new SharedRiderProfileDTO();
        mockRiderProfile.setRiderId("550e8400-e29b-41d4-a716-446655440000");
        mockRiderProfile.setStatus("ACTIVE");
        mockRiderProfile.setRating(BigDecimal.valueOf(4.5));
        mockRiderProfile.setFlags(Collections.emptyList());
        when(riderServiceOutputPort.validateRider(anyString()))
                .thenReturn(mockRiderProfile);

        // Mock Pricing Service (same as successful case)
        SharedFareEstimateDTO mockFareEstimate = new SharedFareEstimateDTO();
        mockFareEstimate.setAmount(new BigDecimal("25.50"));
        mockFareEstimate.setCurrency("USD");
        mockFareEstimate.setSurgeMultiplier(BigDecimal.ONE);
        mockFareEstimate.setEstimatedDuration(1800L);
        mockFareEstimate.setEstimatedDistance(BigDecimal.valueOf(10.5));
        when(pricingServiceOutputPort.estimateFare(any(), any(), any()))
                .thenReturn(mockFareEstimate);

        // Mock Payment Service - Return failed pre-authorization
        SharedPaymentAuthorizationDTO mockPaymentAuth = new SharedPaymentAuthorizationDTO();
        mockPaymentAuth.setAuthorizationId(null); // No authorization ID for failed payments
        mockPaymentAuth.setRiderId("550e8400-e29b-41d4-a716-446655440000");
        SharedMoneyDTO mockMoney = new SharedMoneyDTO();
        mockMoney.setAmount(new BigDecimal("25.50"));
        mockMoney.setCurrency("USD");
        mockPaymentAuth.setAmount(mockMoney);
        mockPaymentAuth.setExpiry(null); // No expiry for failed auth
        mockPaymentAuth.setStatus("FAILED");
        when(paymentServiceOutputPort.preAuthorize(any(), any(), any()))
                .thenReturn(mockPaymentAuth);
        
        System.out.println("✅ Mock responses configured for payment failure scenario");
    }

    private void setupKafkaConsumers() {
        // Setup consumer properties
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID().toString());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ai.shreds.shared.dtos");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SharedTripEventDTO.class.getName());
        
        // Create consumer for trip events
        ConsumerFactory<String, SharedTripEventDTO> tripEventConsumerFactory = 
            new DefaultKafkaConsumerFactory<>(consumerProps);
        tripEventConsumer = tripEventConsumerFactory.createConsumer();
        tripEventConsumer.subscribe(Collections.singletonList("trip-lifecycle-events-test"));
        
        // Create consumer for matching requests
        Map<String, Object> matchingConsumerProps = new HashMap<>(consumerProps);
        matchingConsumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SharedMatchingRequestDTO.class.getName());
        matchingConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-matching-consumer-" + UUID.randomUUID().toString());
        
        ConsumerFactory<String, SharedMatchingRequestDTO> matchingConsumerFactory = 
            new DefaultKafkaConsumerFactory<>(matchingConsumerProps);
        matchingRequestConsumer = matchingConsumerFactory.createConsumer();
        matchingRequestConsumer.subscribe(Collections.singletonList("matching-requests-test"));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> consumeKafkaMessages(Consumer<String, T> consumer, String topic, int expectedCount, Duration timeout) {
        List<T> messages = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        
        while (messages.size() < expectedCount && (System.currentTimeMillis() - startTime) < timeoutMs) {
            ConsumerRecords<String, T> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, T> record : records) {
                messages.add(record.value());
                System.out.println("📨 Consumed message from " + topic + ": " + record.value());
            }
        }
        
        return messages;
    }
}