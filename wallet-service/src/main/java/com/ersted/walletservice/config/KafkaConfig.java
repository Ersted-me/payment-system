package com.ersted.walletservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.producer.deposit}")
    private String depositTopic;

    @Value("${kafka.topics.producer.withdrawal}")
    private String withdrawalTopic;

    @Value("${kafka.topics.consumer.deposit}")
    private String depositConsumerTopic;

    @Value("${kafka.topics.consumer.withdrawal}")
    private String withdrawalConsumerTopic;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public NewTopic depositTopic() {
        return TopicBuilder.name(depositTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalTopic() {
        return TopicBuilder.name(withdrawalTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic depositConsumerTopic() {
        return TopicBuilder.name(depositConsumerTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalConsumerTopic() {
        return TopicBuilder.name(withdrawalConsumerTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

}
