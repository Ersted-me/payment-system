package com.ersted.walletservice.service.strategy;

import com.ersted.walletservice.config.FeeProperties;
import com.ersted.walletservice.dto.kafka.WithdrawalRequestedEvent;
import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.service.OutboxService;
import com.ersted.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static com.ersted.walletservice.entity.TransactionStatus.COMPLETED;
import static com.ersted.walletservice.entity.TransactionStatus.FAILED;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalStrategy implements TransactionStrategy, CompletableStrategy {

    private final FeeProperties feeProperties;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final OutboxService outboxService;

    @Value("${kafka.topics.producer.withdrawal}")
    private String withdrawalTopic;

    @Override
    public PaymentType getType() {
        return PaymentType.WITHDRAWAL;
    }

    @Override
    public Transaction init(InitTransactionRequest request) {
        WithdrawalInitRequest withdrawal = (WithdrawalInitRequest) request;
        log.info("Init withdrawal for walletUuid: [{}]", withdrawal.getWalletUuid());

        Wallet wallet = walletService.getWallet(withdrawal.getWalletUuid());
        BigDecimal fee = calculateFee(withdrawal.getAmount());
        validate(wallet, withdrawal.getAmount(), fee);

        return buildTransaction(
                wallet,
                withdrawal.getAmount(),
                fee,
                withdrawal.getPaymentMethodId(),
                withdrawal.getComment()
        );
    }

    @Override
    public Transaction confirm(ConfirmTransactionRequest request) {
        WithdrawalConfirmRequest withdrawal = (WithdrawalConfirmRequest) request;
        log.info("Confirm withdrawal for walletUuid: [{}]", withdrawal.getWalletUuid());

        Wallet wallet = walletService.getWallet(withdrawal.getWalletUuid());
        BigDecimal fee = calculateFee(withdrawal.getAmount());
        validate(wallet, withdrawal.getAmount(), fee);

        Transaction tx = buildTransaction(
                wallet,
                withdrawal.getAmount(),
                fee,
                withdrawal.getPaymentMethodId(),
                withdrawal.getComment()
        );
        tx.setStatus(com.ersted.walletservice.entity.TransactionStatus.PENDING);

        deductBalance(wallet, withdrawal.getAmount().add(fee));
        transactionRepository.save(tx);

        WithdrawalRequestedEvent event = WithdrawalRequestedEvent.newBuilder()
                .setTransactionId(tx.getUuid())
                .setUserId(tx.getUserUuid())
                .setWalletId(tx.getWallet().getUuid())
                .setAmount(tx.getAmount().setScale(4, RoundingMode.HALF_UP))
                .setCurrency(tx.getWallet().getWalletType().getCurrencyCode())
                .setDestination(withdrawal.getPaymentMethodId().toString())
                .setTimestamp(Instant.now())
                .build();
        outboxService.save(withdrawalTopic, tx.getUuid().toString(), event);

        return tx;
    }

    @Override
    public void complete(UUID transactionId, String status, String failureReason) {
        log.info("Complete withdrawal transactionId: [{}] status: [{}]", transactionId, status);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.warn("Could not find transaction with uuid: [{}]", transactionId);
                    return new ApiException("Could not find transaction with uuid: [%s]".formatted(transactionId));
                });

        if ("COMPLETED".equals(status)) {
            transaction.setStatus(COMPLETED);
        } else {
            transaction.setStatus(FAILED);
            transaction.setFailureReason(failureReason);
            refundWallet(transaction);
        }

        transactionRepository.save(transaction);
    }

    private void refundWallet(Transaction transaction) {
        Wallet wallet = transaction.getWallet();
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount().add(transaction.getFee())));
        walletRepository.save(wallet);
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feeProperties.getWithdrawal());
    }

    private void validate(Wallet wallet, BigDecimal amount, BigDecimal fee) {
        validateWalletActive(wallet);
        validateSufficientFunds(wallet, amount.add(fee));
    }

    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Wallet is not active uuid: [{}]", wallet.getUuid());
            throw new ApiException("Wallet is not active");
        }
    }

    private void validateSufficientFunds(Wallet wallet, BigDecimal totalAmount) {
        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            log.warn("Insufficient funds walletUuid: [{}]", wallet.getUuid());
            throw new ApiException("Insufficient funds");
        }
    }

    private void deductBalance(Wallet wallet, BigDecimal totalAmount) {
        wallet.setBalance(wallet.getBalance().subtract(totalAmount));
        walletRepository.save(wallet);
    }

    private Transaction buildTransaction(
            Wallet wallet,
            BigDecimal amount,
            BigDecimal fee,
            Long paymentMethodId,
            String comment
    ) {
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setUserUuid(wallet.getUserUuid());
        tx.setAmount(amount);
        tx.setFee(fee);
        tx.setPaymentMethodId(paymentMethodId);
        tx.setComment(comment);
        tx.setType(com.ersted.walletservice.entity.PaymentType.WITHDRAWAL);
        return tx;
    }

}
