package com.ersted.walletservice.service.strategy;

import com.ersted.walletservice.config.FeeProperties;
import com.ersted.walletservice.dto.kafka.DepositRequestedEvent;
import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.service.OutboxService;
import com.ersted.walletservice.service.WalletService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.ersted.walletservice.entity.TransactionStatus.COMPLETED;
import static com.ersted.walletservice.entity.TransactionStatus.FAILED;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositStrategy implements TransactionStrategy, CompletableStrategy {

    private final FeeProperties feeProperties;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final OutboxService outboxService;

    @Value("${kafka.topics.producer.deposit}")
    private String depositTopic;

    @Override
    public PaymentType getType() {
        return PaymentType.DEPOSIT;
    }

    @Override
    public Transaction init(InitTransactionRequest request) {
        DepositInitRequest deposit = (DepositInitRequest) request;
        log.info("Init deposit for walletUuid: [{}]", deposit.getWalletUuid());

        return calculate(deposit.getWalletUuid(), deposit.getAmount(),
                deposit.getPaymentMethodId(), deposit.getComment());
    }

    @Override
    public Transaction confirm(ConfirmTransactionRequest request) {
        DepositConfirmRequest deposit = (DepositConfirmRequest) request;
        log.info("Confirm deposit for walletUuid: [{}]", deposit.getWalletUuid());

        Transaction tx = calculate(
                deposit.getWalletUuid(),
                deposit.getAmount(),
                deposit.getPaymentMethodId(),
                deposit.getComment()
        );
        tx.setStatus(com.ersted.walletservice.entity.TransactionStatus.PENDING);
        transactionRepository.save(tx);

        DepositRequestedEvent event = new DepositRequestedEvent(
                tx.getUuid(),
                tx.getUserUuid(),
                tx.getWallet().getUuid(),
                tx.getAmount(),
                tx.getWallet().getWalletType().getCurrencyCode(),
                Instant.now()
        );
        outboxService.save(depositTopic, tx.getUuid().toString(), event);

        return tx;
    }

    @Override
    public void complete(UUID transactionId, String status, String failureReason) {
        log.info("Complete deposit transactionId: [{}] status: [{}]", transactionId, status);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.warn("Could not find transaction with uuid: [{}]", transactionId);
                    return new ApiException("Could not find transaction with uuid: [%s]".formatted(transactionId));
                });

        if ("COMPLETED".equals(status)) {
            creditWallet(transaction);
            transaction.setStatus(COMPLETED);
        } else {
            transaction.setStatus(FAILED);
        }

        transactionRepository.save(transaction);
    }

    private void creditWallet(Transaction transaction) {
        Wallet wallet = transaction.getWallet();
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount().subtract(transaction.getFee())));
        walletRepository.save(wallet);
    }

    private Transaction calculate(UUID walletUuid, BigDecimal amount, Long paymentMethodId, String comment) {
        Wallet wallet = validateWallet(walletUuid);

        BigDecimal fee = amount.multiply(feeProperties.getDeposit());

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setUserUuid(wallet.getUserUuid());
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setPaymentMethodId(paymentMethodId);
        tx.setComment(comment);
        tx.setType(com.ersted.walletservice.entity.PaymentType.DEPOSIT);
        return tx;
    }

    private Wallet validateWallet(UUID walletUuid) {
        Wallet wallet = walletService.getWallet(walletUuid);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Wallet is not active uuid: [{}]", walletUuid);
            throw new ApiException("Wallet is not active");
        }

        return wallet;
    }

}
