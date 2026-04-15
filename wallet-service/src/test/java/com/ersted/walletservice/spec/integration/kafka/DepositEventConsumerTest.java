package com.ersted.walletservice.spec.integration.kafka;

import com.ersted.walletservice.dto.kafka.DepositCompletedEvent;
import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.TransactionStatus;
import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.repository.InboxMessageRepository;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.repository.WalletTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DepositEventConsumerTest extends KafkaLifecycleSpecification {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InboxMessageRepository inboxMessageRepository;

    @Value("${kafka.topics.consumer.deposit}")
    private String depositConsumerTopic;

    private UUID walletTypeUid;

    private final List<UUID> createdTransactionIds = new ArrayList<>();
    private final List<UUID> createdWalletIds = new ArrayList<>();
    private final List<UUID> createdInboxIds = new ArrayList<>();

    @BeforeAll
    void loadWalletType() {
        walletTypeUid = walletTypeRepository.findAll().get(0).getUuid();
    }

    @AfterEach
    void cleanUp() {
        createdTransactionIds.forEach(transactionRepository::deleteById);
        createdTransactionIds.clear();
        createdWalletIds.forEach(walletRepository::deleteById);
        createdWalletIds.clear();
        createdInboxIds.forEach(inboxMessageRepository::deleteById);
        createdInboxIds.clear();
    }

    @Test
    void shouldCompleteDepositAndCreditWallet() throws Exception {
        // Given
        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), BigDecimal.ZERO);
        Transaction tx = createAndSaveDepositTransaction(wallet, new BigDecimal("100.0000"), BigDecimal.ZERO);

        DepositCompletedEvent event = DepositCompletedEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("COMPLETED")
                .setAmount(tx.getAmount())
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(depositConsumerTopic, tx.getUuid().toString(), event).get();

        // Then
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .orElse(false), Duration.ofSeconds(10));

        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, updated.getStatus());

        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();

        assertEquals(0, new BigDecimal("100.00").compareTo(updatedWallet.getBalance()));

        createdInboxIds.add(tx.getUuid());
    }

    @Test
    void shouldFailDepositWhenStatusFailed() throws Exception {
        // Given
        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), BigDecimal.ZERO);
        Transaction tx = createAndSaveDepositTransaction(wallet, new BigDecimal("50.0000"), BigDecimal.ZERO);

        DepositCompletedEvent event = DepositCompletedEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("FAILED")
                .setAmount(tx.getAmount())
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(depositConsumerTopic, tx.getUuid().toString(), event).get();

        // Then
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.FAILED)
                .orElse(false), Duration.ofSeconds(10));

        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.FAILED, updated.getStatus());


        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet.getBalance()));

        createdInboxIds.add(tx.getUuid());
    }

    @Test
    void shouldIgnoreDuplicateDepositEvent() throws Exception {
        // Given
        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), BigDecimal.ZERO);
        Transaction tx = createAndSaveDepositTransaction(wallet, new BigDecimal("75.0000"), BigDecimal.ZERO);

        DepositCompletedEvent event = DepositCompletedEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("COMPLETED")
                .setAmount(tx.getAmount())
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(depositConsumerTopic, tx.getUuid().toString(), event).get();
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .orElse(false), Duration.ofSeconds(10));

        kafkaTemplate.send(depositConsumerTopic, tx.getUuid().toString(), event).get();
        Thread.sleep(1000);

        // Then
        assertTrue(inboxMessageRepository.existsById(tx.getUuid()));
        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, updated.getStatus());


        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();
        assertEquals(0, new BigDecimal("75.00").compareTo(updatedWallet.getBalance()));

        createdInboxIds.add(tx.getUuid());
    }

    private Wallet createAndSaveWallet(UUID userUuid, BigDecimal balance) {
        Wallet wallet = Wallet.builder()
                .walletType(walletTypeRepository.findById(walletTypeUid).orElseThrow())
                .userUuid(userUuid)
                .name("Test Wallet")
                .status(WalletStatus.ACTIVE)
                .balance(balance)
                .build();
        Wallet saved = walletRepository.save(wallet);
        createdWalletIds.add(saved.getUuid());
        return saved;
    }

    private Transaction createAndSaveDepositTransaction(Wallet wallet, BigDecimal amount, BigDecimal fee) {
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setUserUuid(wallet.getUserUuid());
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setType(com.ersted.walletservice.entity.PaymentType.DEPOSIT);
        tx.setStatus(TransactionStatus.PENDING);
        Transaction saved = transactionRepository.save(tx);
        createdTransactionIds.add(saved.getUuid());
        return saved;
    }

}
