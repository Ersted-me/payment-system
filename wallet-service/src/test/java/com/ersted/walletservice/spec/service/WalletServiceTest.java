package com.ersted.walletservice.spec.service;

import com.ersted.walletservice.entity.Wallet;
import com.ersted.walletservice.entity.WalletStatus;
import com.ersted.walletservice.entity.WalletType;
import com.ersted.walletservice.entity.WalletTypeStatus;
import com.ersted.walletservice.exception.ApiException;
import com.ersted.walletservice.mapper.WalletMapper;
import com.ersted.walletservice.model.CreateWalletRequest;
import com.ersted.walletservice.model.WalletResponse;
import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.repository.WalletTypeRepository;
import com.ersted.walletservice.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletTypeRepository walletTypeRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void shouldCreateWalletSuccessfully() {
        // Given
        UUID walletTypeUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest("My Wallet", walletTypeUuid, userUuid);

        WalletType walletType = buildWalletType(walletTypeUuid, WalletTypeStatus.ACTIVE);
        Wallet wallet = buildWallet(walletType, userUuid);
        WalletResponse expectedResponse = new WalletResponse();
        expectedResponse.setUuid(UUID.randomUUID());

        when(walletTypeRepository.findById(walletTypeUuid)).thenReturn(Optional.of(walletType));
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenReturn(wallet);
        when(walletMapper.map(any(Wallet.class))).thenReturn(expectedResponse);

        // When
        WalletResponse result = walletService.create(request);

        // Then
        assertNotNull(result);
        verify(walletTypeRepository).findById(walletTypeUuid);
        verify(walletRepository).saveAndFlush(any(Wallet.class));
        verify(walletMapper).map(any(Wallet.class));
    }

    @Test
    void shouldFailCreateWhenWalletTypeNotFound() {
        // Given
        UUID walletTypeUuid = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest("My Wallet", walletTypeUuid, UUID.randomUUID());

        when(walletTypeRepository.findById(walletTypeUuid)).thenReturn(Optional.empty());

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> walletService.create(request)
        );

        assertTrue(exception.getMessage().contains(walletTypeUuid.toString()));
        verifyNoInteractions(walletRepository, walletMapper);
    }

    @Test
    void shouldFailCreateWhenWalletTypeNotActive() {
        // Given
        UUID walletTypeUuid = UUID.randomUUID();
        CreateWalletRequest request = new CreateWalletRequest("My Wallet", walletTypeUuid, UUID.randomUUID());
        WalletType walletType = buildWalletType(walletTypeUuid, WalletTypeStatus.ARCHIVE);

        when(walletTypeRepository.findById(walletTypeUuid)).thenReturn(Optional.of(walletType));

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> walletService.create(request)
        );

        assertTrue(exception.getMessage().contains(walletTypeUuid.toString()));
        verifyNoInteractions(walletRepository, walletMapper);
    }

    @Test
    void shouldGetWalletInfoSuccessfully() {
        // Given
        UUID walletUuid = UUID.randomUUID();
        Wallet wallet = buildWallet(null, UUID.randomUUID());
        WalletResponse expectedResponse = new WalletResponse();

        when(walletRepository.findById(walletUuid)).thenReturn(Optional.of(wallet));
        when(walletMapper.map(wallet)).thenReturn(expectedResponse);

        // When
        WalletResponse result = walletService.info(walletUuid);

        // Then
        assertNotNull(result);
        verify(walletRepository).findById(walletUuid);
        verify(walletMapper).map(wallet);
    }

    @Test
    void shouldFailGetWalletInfoWhenNotFound() {
        // Given
        UUID walletUuid = UUID.randomUUID();

        when(walletRepository.findById(walletUuid)).thenReturn(Optional.empty());

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> walletService.info(walletUuid)
        );

        assertTrue(exception.getMessage().contains(walletUuid.toString()));
    }

    @Test
    void shouldGetWalletSuccessfully() {
        // Given
        UUID walletUuid = UUID.randomUUID();
        Wallet wallet = buildWallet(null, UUID.randomUUID());

        when(walletRepository.findById(walletUuid)).thenReturn(Optional.of(wallet));

        // When
        Wallet result = walletService.getWallet(walletUuid);

        // Then
        assertNotNull(result);
        assertEquals(wallet, result);
    }

    @Test
    void shouldFailGetWalletWhenNotFound() {
        // Given
        UUID walletUuid = UUID.randomUUID();

        when(walletRepository.findById(walletUuid)).thenReturn(Optional.empty());

        // When / Then
        ApiException exception = assertThrows(
                ApiException.class,
                () -> walletService.getWallet(walletUuid)
        );

        assertTrue(exception.getMessage().contains(walletUuid.toString()));
    }

    @Test
    void shouldGetAllWalletsInfoSuccessfully() {
        // Given
        UUID userUuid = UUID.randomUUID();
        Wallet wallet1 = buildWallet(null, userUuid);
        Wallet wallet2 = buildWallet(null, userUuid);
        List<Wallet> wallets = List.of(wallet1, wallet2);
        List<WalletResponse> expectedResponses = List.of(new WalletResponse(), new WalletResponse());

        when(walletRepository.findAllByUserUuid(userUuid)).thenReturn(wallets);
        when(walletMapper.map(wallets)).thenReturn(expectedResponses);

        // When
        List<WalletResponse> result = walletService.getAllWalletsInfo(userUuid);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(walletRepository).findAllByUserUuid(userUuid);
        verify(walletMapper).map(wallets);
    }

    private WalletType buildWalletType(UUID uuid, WalletTypeStatus status) {
        WalletType walletType = new WalletType();
        walletType.setUuid(uuid);
        walletType.setName("USD Wallet");
        walletType.setCurrencyCode("USD");
        walletType.setStatus(status);
        return walletType;
    }

    private Wallet buildWallet(WalletType walletType, UUID userUuid) {
        return Wallet.builder()
                .uuid(UUID.randomUUID())
                .walletType(walletType)
                .userUuid(userUuid)
                .name("Test Wallet")
                .status(WalletStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();
    }

}
