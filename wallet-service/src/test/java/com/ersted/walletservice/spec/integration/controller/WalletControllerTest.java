package com.ersted.walletservice.spec.integration.controller;

import com.ersted.walletservice.repository.WalletTypeRepository;
import com.ersted.walletservice.spec.integration.LifecycleSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WalletControllerTest extends LifecycleSpecification {

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    private UUID walletTypeUid;

    @BeforeAll
    void loadWalletType() {
        walletTypeUid = walletTypeRepository.findAll().get(0).getUuid();
    }

    @Test
    void shouldCreateWalletSuccessfully() throws Exception {
        UUID userUuid = UUID.randomUUID();

        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(userUuid, walletTypeUid, "Test Wallet")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test Wallet"))
                .andExpect(jsonPath("$.userUuid").value(userUuid.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void shouldFailCreateWhenWalletTypeNotFound() throws Exception {
        UUID unknownTypeUid = UUID.randomUUID();

        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(UUID.randomUUID(), unknownTypeUid, "Test Wallet")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldGetWalletInfoSuccessfully() throws Exception {
        UUID userUuid = UUID.randomUUID();

        String location = mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(userUuid, walletTypeUid, "Info Wallet")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String walletUuid = extractUuidFromLocation(location);

        mockMvc.perform(get("/v1/wallets/{walletUuid}", walletUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(walletUuid))
                .andExpect(jsonPath("$.name").value("Info Wallet"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldGetWalletInfoNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(get("/v1/wallets/{walletUuid}", randomUuid))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldGetAllWalletsInfoForUser() throws Exception {
        UUID userUuid = UUID.randomUUID();

        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(userUuid, walletTypeUid, "Wallet One")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(userUuid, walletTypeUid, "Wallet Two")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/wallets/user/{userUuid}", userUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenNoWalletsForUser() throws Exception {
        UUID userUuid = UUID.randomUUID();

        mockMvc.perform(get("/v1/wallets/user/{userUuid}", userUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String buildCreateWalletRequest(UUID userUuid, UUID walletTypeUid, String name) {
        return """
                {
                  "name": "%s",
                  "walletTypeUid": "%s",
                  "userUuid": "%s"
                }
                """.formatted(name, walletTypeUid, userUuid);
    }

    private String extractUuidFromLocation(String location) {
        if (location == null) {
            throw new IllegalStateException("Location header is missing");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

}
