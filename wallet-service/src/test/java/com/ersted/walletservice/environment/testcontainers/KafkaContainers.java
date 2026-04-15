package com.ersted.walletservice.environment.testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

public class KafkaContainers {

    public static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.POSTGRES;
    public static final KafkaContainer KAFKA = KafkaTestContainer.KAFKA;

    public static void run() {
        POSTGRES.start();
        KAFKA.start();
    }

}
