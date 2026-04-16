package com.ersted.walletservice.service;

import com.ersted.walletservice.entity.OutboxMessage;
import com.ersted.walletservice.entity.OutboxStatus;
import com.ersted.walletservice.repository.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public void save(String topic, String key, Object event) {
        try {
            OutboxMessage message = new OutboxMessage();
            message.setTopic(topic);
            message.setKey(key);
            message.setPayload(objectMapper.writeValueAsString(event));
            message.setPayloadType(event.getClass().getName());
            message.setStatus(OutboxStatus.PENDING);
            message.setRetryCount(0);
            outboxMessageRepository.save(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload for topic: " + topic, e);
        }
    }

}
