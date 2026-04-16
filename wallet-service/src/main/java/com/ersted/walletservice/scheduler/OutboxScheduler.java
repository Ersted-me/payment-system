package com.ersted.walletservice.scheduler;

import com.ersted.walletservice.entity.OutboxMessage;
import com.ersted.walletservice.entity.OutboxStatus;
import com.ersted.walletservice.repository.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final int MAX_RETRIES = 3;

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.scheduler.fixed-delay-ms:5000}")
    @Transactional
    public void process() {
        List<OutboxMessage> messages = outboxMessageRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxMessage message : messages) {
            try {
                Object payload = objectMapper.readValue(message.getPayload(), Class.forName(message.getPayloadType()));
                kafkaTemplate.send(message.getTopic(), message.getKey(), payload).get();

                message.setStatus(OutboxStatus.SENT);
                message.setProcessedAt(OffsetDateTime.now());
                log.info("Outbox message sent id: [{}] topic: [{}]", message.getId(), message.getTopic());
            } catch (Exception e) {
                log.error("Failed to send outbox message id: [{}]", message.getId(), e);
                message.setRetryCount(message.getRetryCount() + 1);
                if (message.getRetryCount() >= MAX_RETRIES) {
                    message.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox message moved to FAILED id: [{}]", message.getId());
                }
            }
        }
    }

}
