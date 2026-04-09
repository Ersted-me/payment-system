package com.ersted.walletservice.dto.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositRequestedEvent(
        UUID transactionId,
        UUID userId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
