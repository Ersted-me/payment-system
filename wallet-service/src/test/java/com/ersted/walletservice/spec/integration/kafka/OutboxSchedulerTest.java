package com.ersted.walletservice.spec.integration.kafka;

import com.ersted.walletservice.dto.kafka.DepositRequestedEvent;
import com.ersted.walletservice.entity.OutboxMessage;
import com.ersted.walletservice.entity.OutboxStatus;
import com.ersted.walletservice.repository.OutboxMessageRepository;
import com.ersted.walletservice.scheduler.OutboxScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxSchedulerTest extends KafkaLifecycleSpecification {

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.producer.deposit}")
    private String depositProducerTopic;

    private final List<UUID> createdOutboxIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdOutboxIds.forEach(outboxMessageRepository::deleteById);
        createdOutboxIds.clear();
    }

    @Test
    void shouldSendPendingOutboxMessageAndMarkAsSent() throws Exception {
        // Given
        DepositRequestedEvent event = DepositRequestedEvent.newBuilder()
                .setTransactionId(UUID.randomUUID())
                .setUserId(UUID.randomUUID())
                .setWalletId(UUID.randomUUID())
                .setAmount(new BigDecimal("100.0000"))
                .setCurrency("USD")
                .setTimestamp(Instant.now())
                .build();

        OutboxMessage message = new OutboxMessage();
        message.setTopic(depositProducerTopic);
        message.setKey(UUID.randomUUID().toString());
        message.setPayload(objectMapper.writeValueAsString(event));
        message.setPayloadType(DepositRequestedEvent.class.getName());
        message.setStatus(OutboxStatus.PENDING);
        message.setRetryCount(0);
        OutboxMessage saved = outboxMessageRepository.save(message);
        createdOutboxIds.add(saved.getId());

        // When
        outboxScheduler.process();

        // Then
        OutboxMessage updated = outboxMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxStatus.SENT, updated.getStatus());
        assertNotNull(updated.getProcessedAt());
        assertEquals(0, updated.getRetryCount());
    }

    @Test
    void shouldNotProcessAlreadySentMessages() throws Exception {
        // Given
        OutboxMessage message = new OutboxMessage();
        message.setTopic(depositProducerTopic);
        message.setKey(UUID.randomUUID().toString());
        message.setPayload("{}");
        message.setPayloadType(DepositRequestedEvent.class.getName());
        message.setStatus(OutboxStatus.SENT);
        message.setRetryCount(0);
        OutboxMessage saved = outboxMessageRepository.save(message);
        createdOutboxIds.add(saved.getId());

        // When
        outboxScheduler.process();

        // Then
        OutboxMessage updated = outboxMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxStatus.SENT, updated.getStatus());
    }

    @Test
    void shouldRetryOnFailureAndMarkAsFailedAfterMaxRetries() throws Exception {
        // Given
        OutboxMessage message = new OutboxMessage();
        message.setTopic(depositProducerTopic);
        message.setKey(UUID.randomUUID().toString());
        message.setPayload("{}");
        message.setPayloadType("com.ersted.walletservice.dto.kafka.NonExistentClass");
        message.setStatus(OutboxStatus.PENDING);
        message.setRetryCount(2);
        OutboxMessage saved = outboxMessageRepository.save(message);
        createdOutboxIds.add(saved.getId());

        // When
        outboxScheduler.process();

        // Then
        OutboxMessage updated = outboxMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, updated.getStatus());
        assertEquals(3, updated.getRetryCount());
    }

    @Test
    void shouldIncrementRetryCountOnFailureBeforeMaxRetries() throws Exception {
        // Given
        OutboxMessage message = new OutboxMessage();
        message.setTopic(depositProducerTopic);
        message.setKey(UUID.randomUUID().toString());
        message.setPayload("{}");
        message.setPayloadType("com.ersted.walletservice.dto.kafka.NonExistentClass");
        message.setStatus(OutboxStatus.PENDING);
        message.setRetryCount(0);
        OutboxMessage saved = outboxMessageRepository.save(message);
        createdOutboxIds.add(saved.getId());

        // When
        outboxScheduler.process();

        // Then
        OutboxMessage updated = outboxMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OutboxStatus.PENDING, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
    }

}
