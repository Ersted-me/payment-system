package com.ersted.walletservice.spec.service;

import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.mapper.TransactionMapper;
import com.ersted.walletservice.model.*;
import com.ersted.walletservice.repository.TransactionRepository;
import com.ersted.walletservice.service.TransactionService;
import com.ersted.walletservice.service.strategy.CompletableStrategy;
import com.ersted.walletservice.service.strategy.TransactionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    interface CompletableDepositStrategy extends TransactionStrategy, CompletableStrategy {}

    @Mock
    private CompletableDepositStrategy depositStrategy;

    @Mock
    private TransactionStrategy transferStrategy;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        when(depositStrategy.getType()).thenReturn(PaymentType.DEPOSIT);
        when(transferStrategy.getType()).thenReturn(PaymentType.TRANSFER);
        transactionService = new TransactionService(
                List.of(depositStrategy, transferStrategy),
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldInitTransactionSuccessfully() {
        // Given
        Transaction calculated = buildTransaction();
        TransactionInitResponse expectedResponse = new TransactionInitResponse();
        DepositInitRequest request = new DepositInitRequest("deposit", UUID.randomUUID(), BigDecimal.TEN, 1L);

        when(depositStrategy.init(request)).thenReturn(calculated);
        when(transactionMapper.mapToInit(calculated)).thenReturn(expectedResponse);

        // When
        TransactionInitResponse result = transactionService.init(PaymentType.DEPOSIT, request);

        // Then
        assertNotNull(result);
        verify(depositStrategy).init(request);
        verify(transactionMapper).mapToInit(calculated);
    }

    @Test
    void shouldConfirmTransactionSuccessfully() {
        // Given
        Transaction confirmed = buildTransaction();
        TransactionConfirmResponse expectedResponse = new TransactionConfirmResponse();
        DepositConfirmRequest request = new DepositConfirmRequest("deposit", UUID.randomUUID(), BigDecimal.TEN, 1L);

        when(depositStrategy.confirm(request)).thenReturn(confirmed);
        when(transactionMapper.mapToConfirm(confirmed)).thenReturn(expectedResponse);

        // When
        TransactionConfirmResponse result = transactionService.confirm(PaymentType.DEPOSIT, request);

        // Then
        assertNotNull(result);
        verify(depositStrategy).confirm(request);
        verify(transactionMapper).mapToConfirm(confirmed);
    }

    @Test
    void shouldGetTransactionStatusSuccessfully() {
        // Given
        UUID transactionUuid = UUID.randomUUID();
        Transaction transaction = buildTransaction();
        TransactionStatusResponse expectedResponse = new TransactionStatusResponse();

        when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.of(transaction));
        when(transactionMapper.map(transaction)).thenReturn(expectedResponse);

        // When
        TransactionStatusResponse result = transactionService.status(transactionUuid);

        // Then
        assertNotNull(result);
        verify(transactionRepository).findById(transactionUuid);
        verify(transactionMapper).map(transaction);
    }

    @Test
    void shouldFailGetTransactionStatusWhenNotFound() {
        // Given
        UUID transactionUuid = UUID.randomUUID();

        when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.empty());

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.status(transactionUuid)
        );

        assertTrue(exception.getMessage().contains(transactionUuid.toString()));
    }

    @Test
    void shouldCompleteTransactionSuccessfully() {
        // Given
        UUID transactionId = UUID.randomUUID();

        // When
        transactionService.complete(PaymentType.DEPOSIT, transactionId, "COMPLETED", null);

        // Then
        verify(depositStrategy).complete(transactionId, "COMPLETED", null);
    }

    @Test
    void shouldFailCompleteWhenTypeDoesNotSupportCompletion() {
        // Given
        UUID transactionId = UUID.randomUUID();

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> transactionService.complete(PaymentType.TRANSFER, transactionId, "COMPLETED", null)
        );

        assertTrue(exception.getMessage().contains("TRANSFER"));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldFindTransactionsSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        Transaction transaction = buildTransaction();
        TransactionStatusResponse statusResponse = new TransactionStatusResponse();

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));
        when(transactionMapper.map(transaction)).thenReturn(statusResponse);

        // When
        List<TransactionStatusResponse> result = transactionService.findTransactions(
                userUuid, null, PaymentType.DEPOSIT, null, null, null, 0, 10
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    private Transaction buildTransaction() {
        Wallet wallet = Wallet.builder()
                .uuid(UUID.randomUUID())
                .userUuid(UUID.randomUUID())
                .name("Test Wallet")
                .status(WalletStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        Transaction tx = new Transaction();
        tx.setUuid(UUID.randomUUID());
        tx.setWallet(wallet);
        tx.setUserUuid(wallet.getUserUuid());
        tx.setAmount(BigDecimal.TEN);
        tx.setFee(BigDecimal.ZERO);
        tx.setType(com.ersted.walletservice.entity.PaymentType.DEPOSIT);
        tx.setStatus(com.ersted.walletservice.entity.TransactionStatus.PENDING);
        return tx;
    }

}
