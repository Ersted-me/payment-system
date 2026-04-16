package com.ersted.walletservice.service;


import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.mapper.TransactionMapper;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.repository.specification.TransactionSpecification;
import com.ersted.walletservice.service.strategy.CompletableStrategy;
import com.ersted.walletservice.service.strategy.TransactionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {

    private final Map<PaymentType, TransactionStrategy> strategies;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;


    public TransactionService(List<TransactionStrategy> strategies,
                              TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper
    ) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(TransactionStrategy::getType, Function.identity()));
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public TransactionInitResponse init(PaymentType type, InitTransactionRequest initTransactionRequest) {
        log.info("Init transaction type: [{}]", type);
        Transaction calculated = strategies.get(type).init(initTransactionRequest);
        return transactionMapper.mapToInit(calculated);
    }

    @Transactional
    public TransactionConfirmResponse confirm(PaymentType type, ConfirmTransactionRequest request) {
        log.info("Confirm transaction type: [{}]", type);
        Transaction transaction = strategies.get(type).confirm(request);
        return transactionMapper.mapToConfirm(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionStatusResponse status(UUID transactionUuid) {
        Transaction transaction = transactionRepository.findById(transactionUuid)
                .orElseThrow(() -> {
                    log.warn("Could not find transaction with uuid: [{}]", transactionUuid);
                    return new ApiException("Could not find transaction with uuid: [%s]".formatted(transactionUuid));
                });

        return transactionMapper.map(transaction);
    }

    @Transactional
    public void complete(PaymentType type, UUID transactionId, String status, String failureReason) {
        log.info("Complete transaction type: [{}] transactionId: [{}]", type, transactionId);
        TransactionStrategy strategy = strategies.get(type);
        if (!(strategy instanceof CompletableStrategy completable)) {
            throw new ApiException("Payment type does not support completion: " + type);
        }
        completable.complete(transactionId, status, failureReason);
    }

    @Transactional(readOnly = true)
    public List<TransactionStatusResponse> findTransactions(
            UUID userUuid,
            UUID walletUuid,
            PaymentType type,
            TransactionStatus status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            int page,
            int size
    ) {

        log.info("Find transactions userUuid: [{}], walletUuid: [{}]", userUuid, walletUuid);

        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());

        Specification<Transaction> spec = TransactionSpecification.filter(
                userUuid,
                walletUuid,
                type == null ? null : com.ersted.walletservice.entity.PaymentType.valueOf(type.getValue()),
                status == null ? null : com.ersted.walletservice.entity.TransactionStatus.valueOf(status.getValue()),
                dateFrom,
                dateTo
        );

        return transactionRepository.findAll(spec, pageable)
                .stream()
                .map(transactionMapper::map)
                .toList();
    }

}
