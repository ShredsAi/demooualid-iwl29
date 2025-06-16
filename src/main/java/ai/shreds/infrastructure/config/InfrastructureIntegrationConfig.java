package ai.shreds.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class InfrastructureIntegrationConfig {

    @Bean
    public MessageChannel tripExecutionChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel matchingRequestChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel matchingResponseChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel tripEventChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel timeoutChannel() {
        return new DirectChannel();
    }
}