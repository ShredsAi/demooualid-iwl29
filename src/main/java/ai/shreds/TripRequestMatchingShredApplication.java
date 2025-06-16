package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@EnableIntegration
@EnableKafka
public class TripRequestMatchingShredApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripRequestMatchingShredApplication.class, args);
    }
}
