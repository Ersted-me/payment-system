package com.ersted.walletservice.dto.kafka;

import java.time.Instant;
import java.util.UUID;

public record WithdrawalResultEvent(
        UUID transactionId,
        String status,
        String failureReason,
        Instant timestamp
) {}