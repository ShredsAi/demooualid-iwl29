package ai.shreds.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Properties;

@Configuration
public class InfrastructureSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("trip-timeout-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }

    @Bean
    public IntegrationFlow integrationFlow() {
        return IntegrationFlow.from("timeoutChannel")
                .handle(message -> {
                    // Handle timeout messages - this will be processed by timeout handlers
                })
                .get();
    }

    private Properties schedulerProperties() {
        Properties properties = new Properties();
        properties.put("scheduler.pool.size", "10");
        properties.put("scheduler.thread.name.prefix", "trip-timeout-scheduler-");
        properties.put("scheduler.await.termination.seconds", "20");
        return properties;
    }
}