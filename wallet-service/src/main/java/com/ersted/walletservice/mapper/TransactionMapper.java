package com.ersted.walletservice.mapper;

import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.model.TransactionConfirmResponse;
import com.ersted.walletservice.model.TransactionInitResponse;
import com.ersted.walletservice.model.TransactionStatusResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionStatusResponse map(Transaction transaction);

    @Mapping(target = "walletUuid", source = "wallet.uuid")
    @Mapping(target = "currencyCode", source = "wallet.walletType.currencyCode")
    TransactionInitResponse mapToInit(Transaction calculated);

    @Mapping(target = "walletUuid", source = "wallet.uuid")
    @Mapping(target = "targetWalletUuid", source = "targetWallet.uuid")
    @Mapping(target = "uid", source = "uuid")
    @Mapping(target = "userUid", source = "userUuid")
    @Mapping(target = "createdAt", source = "created")
    TransactionConfirmResponse mapToConfirm(Transaction transaction);
}
