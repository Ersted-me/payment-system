package com.ersted.walletservice.spec.integration.kafka;

import com.ersted.walletservice.WalletServiceApplication;
import com.ersted.walletservice.environment.testcontainers.KafkaContainers;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {WalletServiceApplication.class}
)
@ActiveProfiles("kafkatest")
public abstract class KafkaLifecycleSpecification {

    static {
        KafkaContainers.run();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", KafkaContainers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", KafkaContainers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", KafkaContainers.POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KafkaContainers.KAFKA::getBootstrapServers);
    }


    protected void waitUntil(Supplier<Boolean> condition, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("Condition not met within " + timeout.getSeconds() + "s timeout");
    }

}
