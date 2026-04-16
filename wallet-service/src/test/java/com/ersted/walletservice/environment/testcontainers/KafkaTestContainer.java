package com.ersted.walletservice.environment.testcontainers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaTestContainer {

    public static final KafkaContainer KAFKA;

    static {
        KAFKA = new KafkaContainer(
                DockerImageName.parse("apache/kafka:4.0.0")
        );
    }

}
