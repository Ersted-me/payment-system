package com.ersted.walletservice.spec.integration.kafka;

import com.ersted.walletservice.dto.kafka.WithdrawalResultEvent;
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

class WithdrawalEventConsumerTest extends KafkaLifecycleSpecification {

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

    @Value("${kafka.topics.consumer.withdrawal}")
    private String withdrawalConsumerTopic;

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
    void shouldCompleteWithdrawalWhenStatusCompleted() throws Exception {
        // Given
        BigDecimal initialBalance = new BigDecimal("400.00");
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = new BigDecimal("1.50"); // 1.5% fee

        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), initialBalance.subtract(amount).subtract(fee));
        Transaction tx = createAndSaveWithdrawalTransaction(wallet, amount, fee);

        WithdrawalResultEvent event = WithdrawalResultEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("COMPLETED")
                .setFailureReason(null)
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(withdrawalConsumerTopic, tx.getUuid().toString(), event).get();

        // Then
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .orElse(false), Duration.ofSeconds(10));

        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, updated.getStatus());
        assertNull(updated.getFailureReason());


        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();
        assertEquals(0, initialBalance.subtract(amount).subtract(fee).compareTo(updatedWallet.getBalance()));

        createdInboxIds.add(tx.getUuid());
    }

    @Test
    void shouldRefundWalletWhenWithdrawalFailed() throws Exception {
        // Given
        BigDecimal deductedBalance = new BigDecimal("298.50"); // after 100 + 1.50 fee deducted from 400
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = new BigDecimal("1.50");

        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), deductedBalance);
        Transaction tx = createAndSaveWithdrawalTransaction(wallet, amount, fee);

        WithdrawalResultEvent event = WithdrawalResultEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("FAILED")
                .setFailureReason("Insufficient funds in payment system")
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(withdrawalConsumerTopic, tx.getUuid().toString(), event).get();

        // Then
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.FAILED)
                .orElse(false), Duration.ofSeconds(10));

        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.FAILED, updated.getStatus());
        assertEquals("Insufficient funds in payment system", updated.getFailureReason());


        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();
        BigDecimal expectedBalance = deductedBalance.add(amount).add(fee);
        assertEquals(0, expectedBalance.compareTo(updatedWallet.getBalance()));

        createdInboxIds.add(tx.getUuid());
    }

    @Test
    void shouldIgnoreDuplicateWithdrawalEvent() throws Exception {
        // Given
        BigDecimal deductedBalance = new BigDecimal("298.50");
        Wallet wallet = createAndSaveWallet(UUID.randomUUID(), deductedBalance);
        Transaction tx = createAndSaveWithdrawalTransaction(wallet, new BigDecimal("100.00"), new BigDecimal("1.50"));

        WithdrawalResultEvent event = WithdrawalResultEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setStatus("COMPLETED")
                .setFailureReason(null)
                .setTimestamp(Instant.now())
                .build();

        // When
        kafkaTemplate.send(withdrawalConsumerTopic, tx.getUuid().toString(), event).get();
        waitUntil(() -> transactionRepository.findById(tx.getUuid())
                .map(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .orElse(false), Duration.ofSeconds(10));

        kafkaTemplate.send(withdrawalConsumerTopic, tx.getUuid().toString(), event).get();
        Thread.sleep(1000);

        // Then
        assertTrue(inboxMessageRepository.existsById(tx.getUuid()));
        Transaction updated = transactionRepository.findById(tx.getUuid()).orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, updated.getStatus());


        Wallet updatedWallet = walletRepository.findById(wallet.getUuid()).orElseThrow();
        assertEquals(0, deductedBalance.compareTo(updatedWallet.getBalance()));

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

    private Transaction createAndSaveWithdrawalTransaction(Wallet wallet, BigDecimal amount, BigDecimal fee) {
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setUserUuid(wallet.getUserUuid());
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setType(com.ersted.walletservice.entity.PaymentType.WITHDRAWAL);
        tx.setStatus(TransactionStatus.PENDING);
        Transaction saved = transactionRepository.save(tx);
        createdTransactionIds.add(saved.getUuid());
        return saved;
    }

}
