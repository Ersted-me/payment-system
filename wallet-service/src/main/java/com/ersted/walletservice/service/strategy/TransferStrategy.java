package com.ersted.walletservice.service.strategy;

import com.ersted.walletservice.config.FeeProperties;
import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.TransactionStatus;
import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferStrategy implements TransactionStrategy {

    private final FeeProperties feeProperties;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public PaymentType getType() {
        return PaymentType.TRANSFER;
    }

    @Override
    public Transaction init(InitTransactionRequest request) {
        TransferInitRequest transfer = (TransferInitRequest) request;
        log.info("Init transfer from walletUuid: [{}] to walletUuid: [{}]",
                transfer.getWalletUuid(), transfer.getTargetWalletUuid());

        return calculate(transfer.getWalletUuid(), transfer.getTargetWalletUuid(),
                transfer.getAmount(), transfer.getComment());
    }

    @Override
    public Transaction confirm(ConfirmTransactionRequest request) {
        TransferConfirmRequest transfer = (TransferConfirmRequest) request;
        log.info("Confirm transfer from walletUuid: [{}] to walletUuid: [{}]",
                transfer.getWalletUuid(), transfer.getTargetWalletUuid());

        Transaction debit = calculate(transfer.getWalletUuid(), transfer.getTargetWalletUuid(),
                transfer.getAmount(), transfer.getComment());
        debit.setStatus(TransactionStatus.COMPLETED);


        Wallet sourceWallet = debit.getWallet();
        Wallet targetWallet = debit.getTargetWallet();
        BigDecimal totalDebit = transfer.getAmount().add(debit.getFee());

        sourceWallet.setBalance(sourceWallet.getBalance().subtract(totalDebit));
        targetWallet.setBalance(targetWallet.getBalance().add(transfer.getAmount()));
        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        Transaction credit = new Transaction();
        credit.setWallet(targetWallet);
        credit.setTargetWallet(sourceWallet);
        credit.setUserUuid(targetWallet.getUserUuid());
        credit.setAmount(transfer.getAmount());
        credit.setFee(BigDecimal.ZERO);
        credit.setComment(transfer.getComment());
        credit.setType(com.ersted.walletservice.entity.PaymentType.TRANSFER);
        credit.setStatus(TransactionStatus.COMPLETED);

        transactionRepository.save(credit);
        transactionRepository.save(debit);

        return debit;
    }

    private Transaction calculate(UUID sourceUuid, UUID targetUuid, BigDecimal amount, String comment) {
        Wallet sourceWallet = validateSourceWallet(sourceUuid, amount);
        Wallet targetWallet = validateTargetWallet(targetUuid);

        BigDecimal fee = amount.multiply(feeProperties.getTransfer());

        Transaction debit = new Transaction();
        debit.setWallet(sourceWallet);
        debit.setTargetWallet(targetWallet);
        debit.setUserUuid(sourceWallet.getUserUuid());
        debit.setAmount(amount);
        debit.setFee(fee);
        debit.setComment(comment);
        debit.setType(com.ersted.walletservice.entity.PaymentType.TRANSFER);
        return debit;
    }

    private Wallet validateSourceWallet(UUID walletUuid, BigDecimal amount) {
        Wallet wallet = walletService.getWallet(walletUuid);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Source wallet is not active uuid: [{}]", walletUuid);
            throw new ApiException("Source wallet is not active");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds in source walletUuid: [{}]", walletUuid);
            throw new ApiException("Insufficient funds in the source wallet");
        }

        return wallet;
    }

    private Wallet validateTargetWallet(UUID walletUuid) {
        Wallet wallet = walletService.getWallet(walletUuid);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Target wallet is not active uuid: [{}]", walletUuid);
            throw new ApiException("Target wallet is not active");
        }

        return wallet;
    }

}
