package com.ersted.walletservice.kafka;

import com.ersted.walletservice.dto.kafka.WithdrawalResultEvent;
import com.ersted.walletservice.model.PaymentType;
import com.ersted.walletservice.service.InboxService;
import com.ersted.walletservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalEventConsumer {

    private final InboxService inboxService;
    private final TransactionService transactionService;

    @Transactional
    @KafkaListener(
            topics = "${kafka.topics.consumer.withdrawal}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload WithdrawalResultEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received WithdrawalResultEvent id: [{}] partition: [{}] offset: [{}]",
                event.getTransactionId(), partition, offset);

        if (inboxService.isDuplicate(event.getTransactionId())) {
            log.warn("Duplicate WithdrawalResultEvent id: [{}], skipping", event.getTransactionId());
            return;
        }

        inboxService.save(event.getTransactionId(), WithdrawalResultEvent.class);

        transactionService.complete(PaymentType.WITHDRAWAL, event.getTransactionId(), event.getStatus(), event.getFailureReason());
    }

}
