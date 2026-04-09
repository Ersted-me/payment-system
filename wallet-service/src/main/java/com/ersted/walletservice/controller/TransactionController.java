package com.ersted.walletservice.controller;

import com.ersted.walletservice.api.TransactionsApi;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TransactionController implements TransactionsApi {

    private final TransactionService transactionService;

    @Override
    public ResponseEntity<List<TransactionStatusResponse>> findTransactions(
            @Nullable String userUid,
            @Nullable String walletUid,
            @Nullable PaymentType type,
            @Nullable TransactionStatus status,
            @Nullable OffsetDateTime dateFrom,
            @Nullable OffsetDateTime dateTo,
            Integer page,
            Integer size
    ) {
        return ResponseEntity.ok(transactionService.findTransactions(
                userUid == null ? null : UUID.fromString(userUid),
                walletUid == null ? null : UUID.fromString(walletUid),
                type,
                status,
                dateFrom, dateTo, page, size));
    }

    @Override
    public ResponseEntity<TransactionInitResponse> initTransaction(
            PaymentType type,
            InitTransactionRequest initTransactionRequest
    ) {
        return ResponseEntity.ok(transactionService.init(type, initTransactionRequest));
    }


    @Override
    public ResponseEntity<TransactionStatusResponse> transactionStatus(UUID transactionUuid) {
        return ResponseEntity.ok(transactionService.status(transactionUuid));
    }

    @Override
    public ResponseEntity<TransactionConfirmResponse> transactionsTypeConfirmPost(
            PaymentType type,
            ConfirmTransactionRequest confirmTransactionRequest
    ) {
        return ResponseEntity.ok(transactionService.confirm(type, confirmTransactionRequest));
    }

}
