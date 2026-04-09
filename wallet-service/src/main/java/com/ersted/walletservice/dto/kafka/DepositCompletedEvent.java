package com.ersted.walletservice.dto.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositCompletedEvent(
        UUID transactionId,
        String status,
        BigDecimal amount,
        Instant timestamp
) {}