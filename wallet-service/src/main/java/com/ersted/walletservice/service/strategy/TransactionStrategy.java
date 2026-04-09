package com.ersted.walletservice.service.strategy;

import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.model.ConfirmTransactionRequest;
import com.ersted.walletservice.model.InitTransactionRequest;
import com.ersted.walletservice.model.PaymentType;

public interface TransactionStrategy {

    PaymentType getType();

    Transaction init(InitTransactionRequest request);

    Transaction confirm(ConfirmTransactionRequest request);


}
