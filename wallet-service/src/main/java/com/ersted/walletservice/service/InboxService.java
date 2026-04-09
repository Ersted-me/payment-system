package com.ersted.walletservice.service;

import com.ersted.walletservice.entity.InboxMessage;
import com.ersted.walletservice.repository.InboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxMessageRepository inboxMessageRepository;

    public boolean isDuplicate(UUID eventId) {
        return inboxMessageRepository.existsById(eventId);
    }

    public void save(UUID eventId, Class<?> eventType) {
        InboxMessage message = new InboxMessage();
        message.setEventId(eventId);
        message.setEventType(eventType.getName());
        inboxMessageRepository.save(message);
    }

}
