package com.ersted.walletservice.dto.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawalRequestedEvent(
        UUID transactionId,
        UUID userId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String destination,
        Instant timestamp
) {}
