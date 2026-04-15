package com.ersted.walletservice.spec.integration.controller;


import com.ersted.walletservice.repository.WalletRepository;
import com.ersted.walletservice.repository.WalletTypeRepository;
import com.ersted.walletservice.spec.integration.LifecycleSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionControllerTest extends LifecycleSpecification {

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletTypeUid;

    @BeforeAll
    void loadWalletType() {
        walletTypeUid = walletTypeRepository.findAll().get(0).getUuid();
    }

    @Test
    void shouldInitDepositSuccessfully() throws Exception {
        UUID walletUuid = createWallet(UUID.randomUUID());

        mockMvc.perform(post("/v1/transactions/DEPOSIT/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildDepositRequest(walletUuid, "100.00", 1001L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletUuid").value(walletUuid.toString()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currencyCode").isNotEmpty());
    }

    @Test
    void shouldConfirmDepositSuccessfully() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID walletUuid = createWallet(userUuid);

        mockMvc.perform(post("/v1/transactions/DEPOSIT/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildDepositRequest(walletUuid, "200.00", 1002L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").isNotEmpty())
                .andExpect(jsonPath("$.walletUuid").value(walletUuid.toString()))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    void shouldInitWithdrawalSuccessfully() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID walletUuid = createWalletWithBalance(userUuid, new BigDecimal("500.00"));

        mockMvc.perform(post("/v1/transactions/WITHDRAWAL/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildWithdrawalRequest(walletUuid, "100.00", 2001L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletUuid").value(walletUuid.toString()))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void shouldFailWithdrawalWhenInsufficientFunds() throws Exception {
        UUID walletUuid = createWallet(UUID.randomUUID());

        mockMvc.perform(post("/v1/transactions/WITHDRAWAL/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildWithdrawalRequest(walletUuid, "100.00", 2002L)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldGetTransactionStatusSuccessfully() throws Exception {
        UUID walletUuid = createWallet(UUID.randomUUID());

        String responseBody = mockMvc.perform(post("/v1/transactions/DEPOSIT/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildDepositRequest(walletUuid, "50.00", 3001L)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transactionUuid = extractField(responseBody, "uid");

        mockMvc.perform(get("/v1/transactions/{transactionUuid}/status", transactionUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetTransactionStatusNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(get("/v1/transactions/{transactionUuid}/status", randomUuid))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldFindTransactionsWithFilters() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID walletUuid = createWallet(userUuid);

        mockMvc.perform(post("/v1/transactions/DEPOSIT/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildDepositRequest(walletUuid, "75.00", 4001L)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/transactions")
                        .param("userUid", userUuid.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnEmptyTransactionListForUnknownUser() throws Exception {
        mockMvc.perform(get("/v1/transactions")
                        .param("userUid", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldConfirmTransferSuccessfully() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID sourceWallet = createWalletWithBalance(userA, new BigDecimal("300.00"));
        UUID targetWallet = createWallet(userB);

        mockMvc.perform(post("/v1/transactions/TRANSFER/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildTransferRequest(sourceWallet, targetWallet, "100.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").isNotEmpty())
                .andExpect(jsonPath("$.walletUuid").value(sourceWallet.toString()))
                .andExpect(jsonPath("$.targetWalletUuid").value(targetWallet.toString()));
    }

    private UUID createWallet(UUID userUuid) throws Exception {
        String location = mockMvc.perform(post("/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateWalletRequest(userUuid, walletTypeUid, "Test Wallet")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        UUID walletUuid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        return walletUuid;
    }

    private UUID createWalletWithBalance(UUID userUuid, BigDecimal balance) throws Exception {
        UUID walletUuid = createWallet(userUuid);
        walletRepository.findById(walletUuid).ifPresent(w -> {
            w.setBalance(balance);
            walletRepository.save(w);
        });
        return walletUuid;
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

    private String buildDepositRequest(UUID walletUuid, String amount, long paymentMethodId) {
        return """
                {
                  "type": "deposit",
                  "walletUuid": "%s",
                  "amount": %s,
                  "paymentMethodId": %d
                }
                """.formatted(walletUuid, amount, paymentMethodId);
    }

    private String buildWithdrawalRequest(UUID walletUuid, String amount, long paymentMethodId) {
        return """
                {
                  "type": "withdrawal",
                  "walletUuid": "%s",
                  "amount": %s,
                  "paymentMethodId": %d
                }
                """.formatted(walletUuid, amount, paymentMethodId);
    }

    private String buildTransferRequest(UUID sourceWallet, UUID targetWallet, String amount) {
        return """
                {
                  "type": "transfer",
                  "walletUuid": "%s",
                  "targetWalletUuid": "%s",
                  "amount": %s
                }
                """.formatted(sourceWallet, targetWallet, amount);
    }

    private String extractField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

}
