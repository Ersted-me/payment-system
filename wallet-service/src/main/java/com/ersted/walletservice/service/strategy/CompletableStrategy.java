package com.ersted.walletservice.service.strategy;

import java.util.UUID;

public interface CompletableStrategy {

    void complete(UUID transactionId, String status, String failureReason);

}
