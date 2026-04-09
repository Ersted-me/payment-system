package com.ersted.walletservice.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletTypeRepository walletTypeRepository;
    private final WalletRepository walletRepository;

    private final WalletMapper walletMapper;

    @Transactional
    public WalletResponse create(CreateWalletRequest request) {
        log.info("Creating wallet userUuid: {}", request.getUserUuid());

        WalletType walletType = walletTypeRepository.findById(request.getWalletTypeUid())
                .orElseThrow(() -> {
                    log.warn("Wallet type not found by uuid: [{}]", request.getWalletTypeUid());
                    return new ApiException("Wallet type not found by uuid: [%s]"
                            .formatted(request.getWalletTypeUid()));
                });

        if (walletType.getStatus() != WalletTypeStatus.ACTIVE) {
            log.warn("Wallet type is not active uuid: [{}]", request.getWalletTypeUid());
            throw new ApiException("Wallet type is not active: [%s]".formatted(request.getWalletTypeUid()));
        }

        Wallet wallet = Wallet.builder()
                .walletType(walletType)
                .userUuid(request.getUserUuid())
                .name(request.getName())
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.saveAndFlush(wallet);

        return walletMapper.map(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse info(UUID walletUuid) {
        log.info("Get wallets info by uuid: [{}]", walletUuid);
        return walletMapper.map(getWallet(walletUuid));
    }

    @Transactional(readOnly = true)
    public @NonNull Wallet getWallet(UUID walletUuid) {
        return walletRepository.findById(walletUuid)
                .orElseThrow(() -> {
                    log.warn("Wallet not found by uuid: [{}]", walletUuid);
                    return new ApiException("Could not find wallet by uuid: [%s]".formatted(walletUuid));
                });
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWalletsInfo(UUID userUuid) {
        log.info("Find all wallets by user uuid: [{}]", userUuid);
        List<Wallet> wallets = walletRepository.findAllByUserUuid(userUuid);
        return walletMapper.map(wallets);
    }

}
