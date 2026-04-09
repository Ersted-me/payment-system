package com.ersted.walletservice.controller;

import com.ersted.walletservice.service.WalletService;
import com.ersted.walletservice.api.WalletsApi;
import com.ersted.walletservice.model.CreateWalletRequest;
import com.ersted.walletservice.model.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class WalletController implements WalletsApi {

    private final WalletService walletService;

    @Override
    public ResponseEntity<WalletResponse> createWallet(CreateWalletRequest createWalletRequest) {
        WalletResponse response = walletService.create(createWalletRequest);

        return ResponseEntity
                .created(URI.create("/v1/wallets/%s".formatted(response.getUuid())))
                .body(response);
    }

    @Override
    public ResponseEntity<List<WalletResponse>> getAllWalletsInfo(UUID userUuid) {
        return ResponseEntity.ok(walletService.getAllWalletsInfo(userUuid));
    }

    @Override
    public ResponseEntity<WalletResponse> getWalletInfo(UUID walletUuid) {
        return ResponseEntity.ok(walletService.info(walletUuid));
    }

}
